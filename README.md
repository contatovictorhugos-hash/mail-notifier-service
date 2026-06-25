# Mail Notifier Service

> Sends transactional email notifications through the Brevo API with optional AES encryption provided by a companion key management service.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Ecosystem](#ecosystem)
- [Contributing](#contributing)

## Overview

Mail Notifier Service accepts email notification requests via a REST endpoint, optionally encrypts the message body using keys fetched from the [Key Management Service](../key-management-service), and dispatches the email through the Brevo (formerly Sendinblue) transactional email API. Every sent email is persisted to a PostgreSQL database with its delivery status, encryption flag, and associated key identifier. Database schema evolution is handled by Flyway migrations.

## Architecture

The application follows a layered architecture with external integration clients:

```
Controller (EmailController)
    ↓
Service (EmailService)
    ├──→ Client (KeyManagementClient)  →  key-management-service :8081
    ├──→ Client (BrevoEmailClient)     →  Brevo API (SMTP/HTTP)
    └──→ Util (CryptoUtils)           →  AES encryption logic
    ↓
Repository (EmailRepository)
    ↓
PostgreSQL (mailnotifierdb)
```

- **EmailController** — Single `POST /emails` endpoint that receives notification requests.
- **EmailService** — Orchestrates the flow: fetch encryption key → encrypt body → send via Brevo → persist result.
- **KeyManagementClient** — HTTP client (RestTemplate) that calls the key-management-service to generate keys.
- **BrevoEmailClient** — HTTP client that dispatches emails through the Brevo API.
- **CryptoUtils** — Utility class for AES encryption of email content.

### Containerisation

A multi-stage `Dockerfile` builds the application with Maven and produces a lightweight JRE image. The `docker-compose.yml` provisions both the application container (port `8080`) and a PostgreSQL 15 Alpine container (port `5432`).

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| ORM | Spring Data JPA (Hibernate) |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Email Provider | Brevo (Sendinblue) transactional API |
| HTTP Client | Apache HttpClient 5, RestTemplate |
| Validation | Jakarta Bean Validation |
| Build | Maven |
| Containerisation | Docker, Docker Compose |

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker and Docker Compose
- A running instance of [key-management-service](../key-management-service) on port `8081`
- A Brevo account with an API key

### Installation

```bash
# Clone the repository
git clone https://github.com/contatovictorhugos/mail-notifier-service.git
cd mail-notifier-service

# Install dependencies
mvn clean install
```

### Configuration

| Variable | Description | Example |
|---|---|---|
| `BREVO_API_KEY` | Brevo transactional email API key | `xkeysib-...` |
| `BREVO_SENDER_EMAIL` | Sender email address registered in Brevo | `noreply@example.com` |
| `BREVO_SENDER_NAME` | Display name for the sender | `Mail Notifier` |
| `KEY_MANAGEMENT_URL` | URL of the key-management-service keys endpoint | `http://localhost:8081/keys` |
| `SPRING_DATASOURCE_URL` | JDBC connection string | `jdbc:postgresql://localhost:5432/mailnotifierdb` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `password` |

Create a `.env` file in the project root with these values (see `.env` for reference).

### Running locally

```bash
# Start the database with Docker Compose
docker compose up -d db

# Ensure key-management-service is running on port 8081

# Run the application
mvn spring-boot:run
```

Or run the full stack in containers:

```bash
docker compose up --build
```

The service starts on **port 8080**.

## Project Structure

```
mail-notifier-service/
├── src/
│   ├── main/
│   │   ├── java/br/com/mailnotifier/
│   │   │   ├── client/          # External service HTTP clients
│   │   │   ├── config/          # Brevo properties and SSL configuration
│   │   │   ├── controller/      # REST endpoints
│   │   │   ├── dto/             # Request/response DTOs
│   │   │   ├── exception/       # Custom exceptions and global handler
│   │   │   ├── model/           # JPA entities (Email, EmailStatus)
│   │   │   ├── repository/      # Spring Data repositories
│   │   │   ├── service/         # Business logic
│   │   │   └── util/            # Cryptography utilities
│   │   └── resources/
│   │       ├── db/migration/    # Flyway SQL migrations
│   │       └── application.yml
│   └── test/                    # Unit and integration tests
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/emails` | Sends a transactional email notification, optionally encrypting the body |

## Ecosystem

This project is part of the **Projetcs** suite. The following projects work together:

| Project | Role | Depends On |
|---|---|---|
| **key-management-service** | REST API — cryptographic key lifecycle management | PostgreSQL |
| **mail-notifier-service** | REST API — transactional email delivery with encryption | key-management-service API, PostgreSQL, Brevo |
| **fipe-csv** | REST API — FIPE vehicle pricing table to CSV export | FIPE public API |
| **bko-project** | Server-rendered web app — internal backoffice administration | PostgreSQL |
| **split-csv** | CLI tool — splits large CSV files into smaller parts | — |
| **mergeCSV** | CLI tool — merges multiple CSV files into one | — |
| **prj_extensao** | Mobile app (React Native / Expo) — Methodist church community app | Firebase |

> **This project**: `mail-notifier-service` consumes the key-management-service API at `http://localhost:8081/keys` to encrypt email bodies before dispatching them through Brevo.

## Contributing

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m 'feat: add your feature'`
4. Push to the branch: `git push origin feature/your-feature-name`
5. Open a Pull Request.

Please follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.
