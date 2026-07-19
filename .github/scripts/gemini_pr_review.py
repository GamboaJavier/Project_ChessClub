#!/usr/bin/env python3
"""Review a pull request diff with Gemini and post a GitHub comment."""

from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request

MAX_DIFF_CHARS = 120_000
GEMINI_ENDPOINT = (
    "https://generativelanguage.googleapis.com/v1beta/models/"
    "{model}:generateContent?key={key}"
)


def require_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise SystemExit(f"Missing required environment variable: {name}")
    return value


def read_diff(path: str) -> str:
    with open(path, encoding="utf-8", errors="replace") as handle:
        diff = handle.read().strip()
    if not diff:
        raise SystemExit("PR diff is empty; nothing to review.")
    if len(diff) > MAX_DIFF_CHARS:
        omitted = len(diff) - MAX_DIFF_CHARS
        diff = (
            diff[:MAX_DIFF_CHARS]
            + f"\n\n...[diff truncated, {omitted} characters omitted]..."
        )
    return diff


def call_gemini(api_key: str, model: str, prompt: str) -> dict:
    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "temperature": 0.2,
            "maxOutputTokens": 8192,
            "thinkingConfig": {"thinkingBudget": 0},
            "responseMimeType": "application/json",
        },
    }
    url = GEMINI_ENDPOINT.format(model=model, key=api_key)
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            body = json.load(response)
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Gemini API error HTTP {exc.code}: {detail}") from exc

    candidates = body.get("candidates") or []
    if not candidates:
        raise RuntimeError(f"Gemini returned no candidates: {json.dumps(body)[:1000]}")

    parts = candidates[0].get("content", {}).get("parts") or []
    text = "".join(part.get("text", "") for part in parts).strip()
    if not text:
        raise RuntimeError(f"Gemini returned empty text: {json.dumps(body)[:1000]}")

    try:
        return json.loads(text)
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"Gemini returned invalid JSON: {text[:1000]}") from exc


def post_pr_comment(repo: str, pr_number: str, body: str, token: str) -> None:
    url = f"https://api.github.com/repos/{repo}/issues/{pr_number}/comments"
    request = urllib.request.Request(
        url,
        data=json.dumps({"body": body}).encode("utf-8"),
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "X-GitHub-Api-Version": "2022-11-28",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            if response.status not in (200, 201):
                raise RuntimeError(f"GitHub comment failed with status {response.status}")
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"GitHub API error HTTP {exc.code}: {detail}") from exc


def build_prompt(diff: str) -> str:
    return f"""You are a senior code reviewer for a Spring Boot project (Java 21, Maven, PostgreSQL, Spring Security).

Review the following pull request diff for the chess club project (Project_ChessClub).

Rules:
- Respond in English only.
- Be concrete and actionable; avoid vague filler.
- Prioritize bugs, security, validation, error handling, design, and missing tests.
- If something looks good, say so briefly.
- Do not invent files or lines that are not in the diff.
- Set has_critical to true ONLY if there is at least one real Critical finding.
- If Critical is None / empty, has_critical must be false.
- review_markdown must use this markdown format:

## Summary
(2-4 sentences)

## Findings
- **Critical:** ...
- **Important:** ...
- **Minor / nit:** ...

## Suggestions
- ...

If a category has no findings, write "None".

Return ONLY valid JSON with this shape:
{{
  "has_critical": boolean,
  "review_markdown": string
}}

DIFF:
```diff
{diff}
```
"""


def has_critical_findings(payload: dict, review_markdown: str) -> bool:
    if isinstance(payload.get("has_critical"), bool):
        flagged = payload["has_critical"]
    else:
        flagged = False

    # Fallback if the model forgets the boolean but still writes Critical findings.
    match = re.search(
        r"\*\*Critical:\*\*\s*(.+?)(?:\n- \*\*|\n## |\Z)",
        review_markdown,
        flags=re.IGNORECASE | re.DOTALL,
    )
    if match:
        value = match.group(1).strip().lower()
        if value and value not in {"none", "n/a", "na", "no", "-", "none."}:
            return True
    return flagged


def main() -> int:
    api_key = require_env("GEMINI_API_KEY")
    token = require_env("GITHUB_TOKEN")
    repo = require_env("REPO")
    pr_number = require_env("PR_NUMBER")
    model = os.environ.get("GEMINI_MODEL", "").strip() or "gemini-flash-latest"
    diff_path = os.environ.get("PR_DIFF_PATH", "pr.diff")

    diff = read_diff(diff_path)

    try:
        payload = call_gemini(api_key, model, build_prompt(diff))
    except RuntimeError as exc:
        # Do not block merges on quota/API outages (free tier).
        message = (
            "## Gemini Code Review\n\n"
            f"_Model: `{model}`_\n\n"
            "Review could not be completed because the Gemini API failed.\n\n"
            f"```\n{exc}\n```\n"
        )
        try:
            post_pr_comment(repo, pr_number, message, token)
        except RuntimeError as comment_exc:
            print(f"Failed to post API-error comment: {comment_exc}", file=sys.stderr)
        print(f"Gemini API unavailable; not failing the check. Detail: {exc}")
        return 0

    review_markdown = str(payload.get("review_markdown") or "").strip()
    if not review_markdown:
        print("Gemini JSON missing review_markdown; failing closed.", file=sys.stderr)
        return 1

    critical = has_critical_findings(payload, review_markdown)
    status = "FAILED (critical findings)" if critical else "PASSED"
    comment = (
        "## Gemini Code Review\n\n"
        f"_Model: `{model}`_\n\n"
        f"**Check status:** {status}\n\n"
        f"{review_markdown}\n"
    )
    post_pr_comment(repo, pr_number, comment, token)
    print("Review posted successfully.")

    if critical:
        print("Critical findings detected; failing the GitHub check.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
