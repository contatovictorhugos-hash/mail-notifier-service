CREATE TABLE tb_emails (
    id UUID PRIMARY KEY,
    destinatario VARCHAR(255) NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    conteudo TEXT,
    status_email VARCHAR(50) NOT NULL,
    data_envio TIMESTAMP NOT NULL
);
