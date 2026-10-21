-- V6: nome do usuario no perfil (opcional, ate 60 caracteres; vazio/branco e gravado como NULL pelo servico).

ALTER TABLE perfil ADD COLUMN nome VARCHAR(60) CHECK (nome IS NULL OR char_length(nome) <= 60);
