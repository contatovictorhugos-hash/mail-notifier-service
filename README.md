# Mail Notifier Service

A Spring Boot-based microservice that acts as an email notifier gateway. It receives email sending requests via a REST endpoint and dispatches them through the **Brevo (formerly Sendinblue) SMTP API**. Each email dispatch is logged into a **PostgreSQL** database with its delivery status. Additionally, it supports **end-to-end hybrid cryptography (RSA-2048 + AES-256)** for secure email delivery.

---

## Features

- **REST API Endpoint**: Simple HTTP POST endpoint to request email dispatch.
- **Brevo Integration**: Uses RestClient to integrate directly with the Brevo SMTP API.
- **Database Persistence**: Keeps track of all emails sent, pending, or failed inside a PostgreSQL database.
- **Flyway Migrations**: Automatic schema creation and evolution using Flyway migrations.
- **Dockerized Environment**: Fully containerized setup for PostgreSQL database and Spring Boot application using Docker Compose.
- **SSL Validation Bypass for Development**: Configured with a `dev` profile that disables strict SSL certificate validation (avoiding PKIX certificate path issues in restricted local environments).
- **Custom Global Exception Handling**: Returns clean, consistent error responses (`ApiError`) in case of failure.
- **Optional Hybrid Cryptography (RSA + AES)**: Securely encrypts email content before transmission and database persistence when a public key is provided.

---

## Hybrid Cryptography Architecture

When a request contains a `publicKey` (RSA PEM format), the service secures the communication using a hybrid asymmetric/symmetric cryptography flow:

1. **Symmetric Key Generation**: The service generates a unique, secure, random 256-bit AES session key and a random 16-byte Initialization Vector (IV) for the request.
2. **Payload Encryption**: The email content is encrypted with the AES session key using `AES/CBC/PKCS5Padding` and the generated IV.
3. **Key Encapsulation (Asymmetric)**: The AES session key is encrypted using the recipient's RSA public key (`RSA/ECB/PKCS1Padding`).
4. **Transmission Payload**: The final content dispatched to the recipient via Brevo SMTP is formatted as:
   `[Base64_Encrypted_AES_Key].[Base64_IV_and_Encrypted_Content]`
5. **Database Security & Privacy**: 
   - The database records the encrypted content (Base64) and sets the `encrypted` flag to `true`.
   - **Zero Knowledge**: The encrypted AES key and RSA public key are **never** persisted in the database logs or schema, ensuring the database owner cannot decrypt the content.
6. **Decryption**: The recipient splits the received email payload at the `.` character, decrypts the AES key using their RSA private key, and then decrypts the email body using the AES key and IV.

---

## Tech Stack

- **Java**: 21
- **Framework**: Spring Boot 3.3.0
- **Database**: PostgreSQL 15 (Alpine)
- **Migrations**: Flyway
- **Containerization**: Docker & Docker Compose
- **HTTP Client**: Apache HttpClient 5 (for connection and custom SSL socket factory)
- **Build Tool**: Maven

---

## Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- A Brevo Account and API Key

### Configuration

The application is configured via environment variables. To run the application, create a `.env` file in the root directory of the project (this file is ignored by Git for security):

```env
# Credenciais do Brevo (Não envie este arquivo para o Git!)
BREVO_API_KEY=sua_api_key_aqui
BREVO_SENDER_EMAIL=contato.victorhugos@gmail.com
BREVO_SENDER_NAME=Mail Notifier
```

### Database Configuration

The PostgreSQL database is configured via Docker Compose:
- **JDBC URL (Local fallback)**: `jdbc:postgresql://localhost:5432/mailnotifierdb`
- **Username**: `postgres`
- **Password**: `password`
- **Port**: `5432`

#### Database Schema (`tb_emails`)
```sql
CREATE TABLE tb_emails (
    id UUID PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    content TEXT,
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    encrypted BOOLEAN DEFAULT FALSE
);
```

---

## Running the Application

### Using Docker Compose (Recommended)

1. Ensure Docker Desktop is open and running.
2. Build and start the services:
   ```bash
   docker compose up --build -d
   ```
3. Follow the application logs to ensure it started successfully:
   ```bash
   docker compose logs -f app
   ```

To stop the services:
```bash
docker compose down
```

### Checking the Database

To check if the database tables were created successfully inside the PostgreSQL container, run:
```bash
docker exec -it mail-notifier-db psql -U postgres -d mailnotifierdb -c "\dt"
```

To see the rows/emails sent inside the database, run:
```bash
docker exec -it mail-notifier-db psql -U postgres -d mailnotifierdb -c "SELECT * FROM tb_emails;"
```

---

## API Documentation

### Send Email

Send a POST request to deliver an email (either in plaintext or encrypted using a public key).

- **URL**: `/emails`
- **Method**: `POST`
- **Content-Type**: `application/json`

#### Request Body (Plaintext)
```json
{
  "recipient": "recipient@example.com",
  "subject": "Welcome to Mail Notifier",
  "content": "<h1>Hello!</h1><p>This is a test notification.</p>"
}
```

#### Request Body (Encrypted - Asymmetric/Symmetric Hybrid)
To send an email with client-side encrypted hybrid payload, you can pass a `publicKey` (PEM format). The service will generate a random AES-256 session key, encrypt the content with AES, encrypt the AES key with the provided RSA public key, and send both to the recipient.
```json
{
  "recipient": "recipient@example.com",
  "subject": "Encrypted Test",
  "content": "<h1>Hello Secure World!</h1><p>This message will be encrypted.</p>",
  "publicKey": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAv...\n-----END PUBLIC KEY-----"
}
```

#### Success Response
- **Status**: `201 Created`
- **Response Body**:
```json
{
  "id": "a1b2c3d4-e5f6-7a8b-9c0d-e1f2a3b4c5d6",
  "recipient": "recipient@example.com",
  "subject": "Welcome to Mail Notifier",
  "status": "SENT",
  "sentAt": "2026-06-22T21:22:16",
  "encrypted": false
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
