# Mail Notifier Service

A Spring Boot-based microservice that acts as an email notifier gateway. It receives email sending requests via a REST endpoint and dispatches them through the **Brevo (formerly Sendinblue) SMTP API**. Each email dispatch is logged into a **PostgreSQL** database with its delivery status. Additionally, it supports **end-to-end hybrid cryptography (RSA-2048 + AES-256)** for secure email delivery, delegating key generation to the companion **[key-management-service](https://github.com/contatovictorhugos/key-management-service)**.

---

## Architecture Overview

The `mailNotifierService` operates as part of a **two-microservice architecture** for secure email delivery:

```
┌──────────────────┐         ┌──────────────────────────┐
│   Client / API   │         │  key-management-service  │
│    Consumer       │         │  (Port 8081)             │
└────────┬─────────┘         │                          │
         │ POST /emails      │  - Generates RSA-2048    │
         │ { encrypted:true }│    key pairs (keyId +    │
         ▼                   │    publicKey + privateKey)│
┌──────────────────┐         │  - Stores private keys   │
│ mailNotifierService│◄──────┤  - Decrypts payloads     │
│ (Port 8080)       │ POST   │    via POST /keys/decrypt│
│                   │ /keys  └──────────────────────────┘
│ - Hybrid encrypt  │
│ - Send via Brevo  │
│ - Persist to DB   │
└──────────────────┘
```

| Service | Responsibility | Default Port |
|---|---|---|
| **mailNotifierService** | Receives email requests, performs hybrid encryption (AES+RSA), sends via Brevo SMTP, persists to PostgreSQL | `8080` |
| **key-management-service** | Generates and stores RSA-2048 key pairs, exposes public keys, decrypts payloads using stored private keys | `8081` |

---

## Features

- **REST API Endpoint**: Simple HTTP POST endpoint to request email dispatch.
- **Brevo Integration**: Uses RestClient to integrate directly with the Brevo SMTP API.
- **Database Persistence**: Keeps track of all emails sent, pending, or failed inside a PostgreSQL database.
- **Flyway Migrations**: Automatic schema creation and evolution using Flyway migrations.
- **Dockerized Environment**: Fully containerized setup for PostgreSQL database and Spring Boot application using Docker Compose.
- **SSL Validation Bypass for Development**: Configured with a `dev` profile that disables strict SSL certificate validation (avoiding PKIX certificate path issues in restricted local environments).
- **Custom Global Exception Handling**: Returns clean, consistent error responses (`ApiError`) in case of failure.
- **Hybrid Cryptography (RSA + AES) with Key Management**: When `encrypted=true`, the service integrates with the `key-management-service` to generate a unique key pair, encrypts the email content using hybrid cryptography, and includes the `keyId` in the response for future decryption.

---

## Hybrid Cryptography Flow

When a request is sent with `"encrypted": true`, the service coordinates with the **key-management-service** to secure the communication using a hybrid asymmetric/symmetric cryptography flow:

1. **Key Generation (via key-management-service)**: The `mailNotifierService` makes an HTTP `POST` to `key-management-service` at `/keys`. The key-management-service generates a new RSA-2048 key pair, stores the private key in its own database, and returns the `keyId` (UUID) and `publicKey` (Base64).
2. **Symmetric Key Generation**: The service generates a unique, secure, random 256-bit AES session key and a random 16-byte Initialization Vector (IV).
3. **Payload Encryption**: The email content is encrypted with the AES session key using `AES/CBC/PKCS5Padding` and the generated IV.
4. **Key Encapsulation (Asymmetric)**: The AES session key is encrypted using the RSA public key received from the key-management-service (`RSA/ECB/PKCS1Padding`).
5. **Transmission Payload**: The final content dispatched to the recipient via Brevo SMTP is formatted as:
   ```
   KeyId.EncryptedAESKey.EncryptedContent
   ```
   Where each part is Base64-encoded and separated by `.` (dot).
6. **Database Persistence**:
   - The database records the **encrypted content** (Base64) and the associated `keyId`.
   - The `encrypted` flag is set to `true`.
   - The AES key and RSA keys are **never** persisted in this service's database.
7. **Decryption (via key-management-service)**: To decrypt the email, the payload `KeyId.EncryptedAESKey.EncryptedContent` is sent to the key-management-service's `POST /keys/decrypt` endpoint. The service splits the payload, retrieves the private key by `keyId`, decrypts the AES key using RSA, and then decrypts the email body using the AES key and IV.

### When `encrypted = false` (or omitted)

The email is processed, saved in the database, and sent in plaintext (standard flow). The `keyId` field in the response will be `null`.

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
- The **key-management-service** running on port `8081` (required only for encrypted email flow)

### Configuration

The application is configured via environment variables. To run the application, create a `.env` file in the root directory of the project (this file is ignored by Git for security):

```env
BREVO_API_KEY=your_api_key_here
BREVO_SENDER_EMAIL=contato.victorhugos@gmail.com
BREVO_SENDER_NAME=Mail Notifier
```

| Variable | Description | Default |
|---|---|---|
| `BREVO_API_KEY` | Your Brevo SMTP API key | *(required)* |
| `BREVO_SENDER_EMAIL` | Sender email address | *(required)* |
| `BREVO_SENDER_NAME` | Sender display name | *(required)* |
| `KEY_MANAGEMENT_URL` | URL of the key-management-service keys endpoint | `http://localhost:8081/keys` |
| `SPRING_DATASOURCE_URL` | JDBC URL for PostgreSQL | `jdbc:postgresql://localhost:5432/mailnotifierdb` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `password` |

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
    encrypted BOOLEAN DEFAULT FALSE,
    key_id UUID
);
```

---

## Running the Application

### Using Docker Compose (Recommended)

1. Ensure Docker Desktop is open and running.
2. Ensure the **key-management-service** is running on port `8081` (if you intend to use the encrypted flow).
3. Build and start the services:
   ```bash
   docker compose up --build -d
   ```
4. Follow the application logs to ensure it started successfully:
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

Send a POST request to deliver an email (either in plaintext or encrypted).

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

#### Request Body (Encrypted — Hybrid RSA + AES via key-management-service)
To send an encrypted email, set `encrypted` to `true`. The service will automatically request a new key pair from the `key-management-service`, perform hybrid encryption, and include the `keyId` in the response.
```json
{
  "recipient": "recipient@example.com",
  "subject": "Encrypted Test",
  "content": "<h1>Hello Secure World!</h1><p>This message will be encrypted.</p>",
  "encrypted": true
}
```

#### Success Response (Plaintext)
- **Status**: `201 Created`
```json
{
  "id": "a1b2c3d4-e5f6-7a8b-9c0d-e1f2a3b4c5d6",
  "recipient": "recipient@example.com",
  "subject": "Welcome to Mail Notifier",
  "status": "SENT",
  "sentAt": "2026-06-22T21:22:16",
  "encrypted": false,
  "keyId": null
}
```

#### Success Response (Encrypted)
- **Status**: `201 Created`
```json
{
  "id": "a1b2c3d4-e5f6-7a8b-9c0d-e1f2a3b4c5d6",
  "recipient": "recipient@example.com",
  "subject": "Encrypted Test",
  "status": "SENT",
  "sentAt": "2026-06-22T21:22:16",
  "encrypted": true,
  "keyId": "f7e8d9c0-b1a2-3456-7890-abcdef123456"
}
```

#### Error Response (Brevo Delivery failure)
- **Status**: `502 Bad Gateway`
```json
{
  "status": 502,
  "error": "Bad Gateway",
  "message": "Falha ao enviar e-mail para recipient@example.com",
  "timestamp": "2026-06-22T21:22:16.123456"
}
```

#### Error Response (Input Validation failure)
- **Status**: `400 Bad Request`
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: recipient: Recipient email is required",
  "timestamp": "2026-06-22T21:22:16.123456"
}
```

#### Error Response (key-management-service unavailable)
- **Status**: `500 Internal Server Error`
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Falha ao enviar e-mail para recipient@example.com",
  "timestamp": "2026-06-22T21:22:16.123456"
}
```

---

## Related Services

| Service | Repository | Description |
|---|---|---|
| **key-management-service** | [GitHub](https://github.com/contatovictorhugos/key-management-service) | RSA key pair generation, storage, and payload decryption |
