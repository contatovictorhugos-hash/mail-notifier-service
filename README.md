# Mail Notifier Service

A Spring Boot-based microservice that acts as an email notifier gateway. It receives email sending requests via a REST endpoint and dispatches them through the **Brevo (formerly Sendinblue) SMTP API**. Each email dispatch is logged into an in-memory database with its delivery status.

---

## Features

- **REST API Endpoint**: Simple HTTP POST endpoint to request email dispatch.
- **Brevo Integration**: Uses RestClient to integrate directly with the Brevo SMTP API.
- **Database Persistence**: Keeps track of all emails sent, pending, or failed inside an H2 in-memory database.
- **SSL Validation Bypass for Development**: Configured with a `dev` profile that disables strict SSL certificate validation (avoiding PKIX certificate path issues in restricted local environments).
- **Custom Global Exception Handling**: Returns clean, consistent error responses (`ApiError`) in case of failure.

---

## Tech Stack

- **Java**: 21
- **Framework**: Spring Boot 3.3.0
- **Database**: H2 Database (In-Memory)
- **HTTP Client**: Apache HttpClient 5 (for connection and custom SSL socket factory)
- **Build Tool**: Maven

---

## Getting Started

### Prerequisites

- Java 21 JDK (e.g. Microsoft OpenJDK or Eclipse Temurin)
- Maven 3.x
- A Brevo Account and API Key

### Configuration

The application is configured via `src/main/resources/application.yml`. You can customize the Brevo configuration using environment variables:

| Variable | Description | Default Value |
|---|---|---|
| `BREVO_API_KEY` | Your Brevo API Key v3 | *Fallback key provided in application.yml* |
| `BREVO_SENDER_EMAIL` | Verified sender email address | `contato.victorhugos@gmail.com` |
| `BREVO_SENDER_NAME` | Name displayed as sender | `Mail Notifier` |

### Database Configuration

For development convenience, H2 database uses a custom credential set:
- **JDBC URL**: `jdbc:h2:mem:mailnotifierdb`
- **Username**: `user`
- **Password**: `password`
- **H2 Console Path**: `http://localhost:8080/h2-console`

---

## API Documentation

### Send Email

Send a POST request to deliver an email.

- **URL**: `/emails`
- **Method**: `POST`
- **Content-Type**: `application/json`

#### Request Body
```json
{
  "destinatario": "recipient@example.com",
  "titulo": "Welcome to Mail Notifier",
  "conteudo": "<h1>Hello!</h1><p>This is a test notification.</p>"
}
```

#### Success Response
- **Status**: `200 OK`
- **Response Body**:
```json
{
  "id": "a1b2c3d4-e5f6-7a8b-9c0d-e1f2a3b4c5d6",
  "destinatario": "recipient@example.com",
  "titulo": "Welcome to Mail Notifier",
  "conteudo": "<h1>Hello!</h1><p>This is a test notification.</p>",
  "dataEnvio": "2026-06-22T21:22:16",
  "statusEmail": "ENVIADO"
}
```

#### Error Response (e.g. Brevo Delivery failure)
- **Status**: `502 Bad Gateway`
- **Response Body**:
```json
{
  "status": 502,
  "error": "Bad Gateway",
  "message": "Falha ao enviar e-mail para recipient@example.com",
  "timestamp": "2026-06-22T21:22:16.123456"
}
```

---

## Running the Application

1. Compile the project:
   ```bash
   mvn clean compile
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The server will start on port `8080`.
