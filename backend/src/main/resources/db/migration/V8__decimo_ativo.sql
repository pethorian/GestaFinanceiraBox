-- V8: 13o salario fixo (fev 50% / nov 50%, constante no codigo) com flag para contar ou nao na projecao.
-- decimo_ativo padrao TRUE: quem ja usava o 13o continua com a mesma projecao.
-- As linhas DECIMO_TERCEIRO de evento_salarial ficam mortas (as parcelas deixam de ser editaveis); so o PLR usa a tabela.

ALTER TABLE perfil ADD COLUMN decimo_ativo BOOLEAN NOT NULL DEFAULT TRUE;

DELETE FROM evento_salarial WHERE tipo = 'DECIMO_TERCEIRO';
ALTER TABLE evento_salarial DROP CONSTRAINT evento_salarial_tipo_check;
ALTER TABLE evento_salarial ADD CONSTRAINT evento_salarial_tipo_check CHECK (tipo IN ('PLR'));
