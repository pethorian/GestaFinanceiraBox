-- V9: banco onde o dinheiro da meta está guardado (texto livre, opcional, até 80 caracteres;
-- o serviço apara espaços e grava vazio/branco como NULL). Sem lista pré-definida.

ALTER TABLE meta ADD COLUMN banco VARCHAR(80) CHECK (banco IS NULL OR char_length(banco) <= 80);
