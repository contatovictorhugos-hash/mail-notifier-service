CREATE TABLE tb_emails (
    id UUID PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    content TEXT,
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    encrypted BOOLEAN DEFAULT FALSE
);
