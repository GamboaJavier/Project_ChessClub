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
NONE_VALUES = {"none", "n/a", "na", "no", "-", "none.", "n/a."}
BLOCKING_LINE_RE = re.compile(
    r"(?im)^\s*BLOCKING:\s*(true|false)\s*$"
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


def call_gemini(api_key: str, model: str, prompt: str) -> str:
    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "temperature": 0.2,
            "maxOutputTokens": 8192,
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
    return text


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
- Use this markdown format for the review body:

## Summary
(2-4 sentences)

## Findings
- **Critical:** ...
- **Important:** ...
- **Minor / nit:** ...

## Suggestions
- ...

If a category has no findings, write "None".

After the markdown review, add exactly one final line (machine-readable, required):
BLOCKING: true
or
BLOCKING: false

Set BLOCKING to true if and only if there is at least one real Critical finding.
Set BLOCKING to false if Critical is None / empty.

DIFF:
```diff
{diff}
```
"""


def extract_blocking_decision(review_text: str) -> tuple[str, bool | None]:
    """Return (comment_markdown, explicit_blocking_or_None)."""
    matches = list(BLOCKING_LINE_RE.finditer(review_text))
    if not matches:
        return review_text.strip(), None

    last = matches[-1]
    blocking = last.group(1).lower() == "true"
    cleaned = BLOCKING_LINE_RE.sub("", review_text).strip()
    return cleaned, blocking


def _section_has_real_findings(section_body: str) -> bool:
    body = section_body.strip()
    if not body:
        return False

    lowered = body.lower()
    if lowered in NONE_VALUES:
        return False

    # Bullet list under a Critical heading.
    bullets = re.findall(r"(?m)^\s*[-*]\s+(.+)$", body)
    if bullets:
        return any(bullet.strip().lower() not in NONE_VALUES for bullet in bullets)

    # Single-line / inline Critical content.
    first_line = body.splitlines()[0].strip().lower()
    return first_line not in NONE_VALUES


def has_critical_findings(review_markdown: str) -> bool:
    """Detect Critical findings across common Gemini markdown shapes."""
    # Shape A: "- **Critical:** ..."
    inline = re.search(
        r"(?is)\*\*Critical:\*\*\s*(.+?)(?=\n\s*-\s*\*\*|\n#{1,3}\s|\n##\s|\Z)",
        review_markdown,
    )
    if inline and _section_has_real_findings(inline.group(1)):
        return True

    # Shape B: "### Critical" / "## Critical:" + following bullets until next heading.
    heading = re.search(
        r"(?im)^#{1,6}\s*Critical:?\s*$",
        review_markdown,
    )
    if heading:
        after = review_markdown[heading.end() :]
        next_heading = re.search(r"(?m)^#{1,6}\s+", after)
        section = after[: next_heading.start()] if next_heading else after
        if _section_has_real_findings(section):
            return True

    return False


def should_block_merge(review_text: str) -> tuple[str, bool]:
    comment_markdown, explicit = extract_blocking_decision(review_text)
    if explicit is not None:
        return comment_markdown, explicit
    return comment_markdown, has_critical_findings(comment_markdown)


def main() -> int:
    api_key = require_env("GEMINI_API_KEY")
    token = require_env("GITHUB_TOKEN")
    repo = require_env("REPO")
    pr_number = require_env("PR_NUMBER")
    model = os.environ.get("GEMINI_MODEL", "").strip() or "gemini-flash-latest"
    diff_path = os.environ.get("PR_DIFF_PATH", "pr.diff")

    diff = read_diff(diff_path)

    try:
        review = call_gemini(api_key, model, build_prompt(diff))
    except RuntimeError as exc:
        # Real API/transport failure only. Do not block merges on free-tier outages.
        print(f"Gemini API unavailable; not failing the check. Detail: {exc}")
        message = (
            "## Gemini Code Review\n\n"
            f"_Model: `{model}`_\n\n"
            "Could not reach the Gemini API, so this review was skipped.\n"
        )
        try:
            post_pr_comment(repo, pr_number, message, token)
        except RuntimeError as comment_exc:
            print(f"Failed to post skip comment: {comment_exc}", file=sys.stderr)
        return 0

    comment_markdown, blocking = should_block_merge(review)
    comment = (
        "## Gemini Code Review\n\n"
        f"_Model: `{model}`_\n\n"
        f"{comment_markdown}\n"
    )
    post_pr_comment(repo, pr_number, comment, token)
    print("Review posted successfully.")

    # Blocking is a review decision, not an API error: fail the Actions check only.
    if blocking:
        print("Critical findings detected; failing the GitHub check.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
