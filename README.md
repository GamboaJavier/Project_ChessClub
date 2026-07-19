# Chess_Club
# Chess Club Backend

The backend system for the university open chess tournament, designed to manage player registrations, tournament logic, and match processing. This project is built with professional software engineering practices, prioritizing modularity, security, and maintainability.

## Project Overview
This platform serves as the core infrastructure for the university's chess community. It handles player data, ensures data integrity through validation, and provides a RESTful API for frontend consumption.

## Tech Stack
- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.x
- **Persistence:** Spring Data JPA / PostgreSQL
- **Security:** Spring Security
- **Data Validation:** Jakarta Validation
- **Architecture:** MVC Pattern (Model-View-Controller) with DTO implementation

## Key Features
- **RESTful API:** Clean endpoints for player management.
- **Data Integrity:** Strict validation using `Jakarta Validation` constraints.
- **DTO Pattern:** Secure decoupling of internal entities and external API responses.
- **Scalable Architecture:** Built with future extensibility for Elo calculation and tournament matching logic.

## Project Structure
- `Controller`: Handles incoming REST requests and directs traffic.
- `Service`: Contains the core business logic.
- `Repository`: Manages database operations using Spring Data JPA.
- `DTO`: Data Transfer Objects for secure and efficient API communication.
- `Entity`: Database models reflecting the tournament domain.

## Roadmap
- [x] Initial setup and PostgreSQL integration.
- [x] Basic CRUD operations for `Player`.
- [ ] Tournament and Match domain modeling.
- [ ] Elo rating calculation system.
- [ ] Frontend integration and API consumption.
- [ ] Currency conversion features (USDT/VES).

---
*Developed by Javier Alejandro Gamboa | CactusMcCoy Dev*
