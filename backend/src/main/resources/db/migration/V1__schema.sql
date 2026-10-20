-- Parâmetros (tabelas com vigência)
CREATE TABLE cargo (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL UNIQUE,
    jornada     SMALLINT     NOT NULL CHECK (jornada IN (6, 8)),
    usa_delta   BOOLEAN      NOT NULL DEFAULT FALSE,
    ordem       INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE cargo_valor (
    id               BIGSERIAL PRIMARY KEY,
    cargo_id         BIGINT        NOT NULL REFERENCES cargo (id) ON DELETE CASCADE,
    piso             NUMERIC(12,2) NOT NULL CHECK (piso >= 0),
    gratificacao     NUMERIC(12,2) NOT NULL CHECK (gratificacao >= 0),
    vigencia_inicio  DATE          NOT NULL,
    vigencia_fim     DATE,
    UNIQUE (cargo_id, vigencia_inicio),
    CHECK (vigencia_fim IS NULL OR vigencia_fim >= vigencia_inicio)
);

CREATE TABLE delta_salarial (
    id               BIGSERIAL PRIMARY KEY,
    referencia       INTEGER       NOT NULL,
    valor            NUMERIC(12,2) NOT NULL CHECK (valor >= 0),
    vigencia_inicio  DATE          NOT NULL,
    vigencia_fim     DATE,
    UNIQUE (referencia, vigencia_inicio),
    CHECK (vigencia_fim IS NULL OR vigencia_fim >= vigencia_inicio)
);

CREATE TABLE tabela_inss_faixa (
    id               BIGSERIAL PRIMARY KEY,
    ate              NUMERIC(12,2) NOT NULL,
    aliquota         NUMERIC(7,5)  NOT NULL,
    deducao          NUMERIC(12,2) NOT NULL,
    vigencia_inicio  DATE          NOT NULL,
    vigencia_fim     DATE
);

CREATE TABLE tabela_irpf_faixa (
    id               BIGSERIAL PRIMARY KEY,
    ate              NUMERIC(12,2),            -- NULL = sem limite superior
    aliquota         NUMERIC(7,5)  NOT NULL,
    deducao          NUMERIC(12,2) NOT NULL,
    vigencia_inicio  DATE          NOT NULL,
    vigencia_fim     DATE
);

CREATE TABLE parametro_beneficio (
    id                       BIGSERIAL PRIMARY KEY,
    vigencia_inicio          DATE          NOT NULL UNIQUE,
    vigencia_fim             DATE,
    pai                      NUMERIC(12,2) NOT NULL,
    home_office              NUMERIC(12,2) NOT NULL,
    vr_va                    NUMERIC(12,2) NOT NULL,
    vt_referencia            NUMERIC(12,2) NOT NULL,
    vt_percentual            NUMERIC(7,5)  NOT NULL,
    sindicato_percentual     NUMERIC(7,5)  NOT NULL,
    sindicato_teto           NUMERIC(12,2) NOT NULL,
    saude_percentual         NUMERIC(7,5)  NOT NULL,
    saude_teto_percentual    NUMERIC(7,5)  NOT NULL,
    plano_por_dependente     NUMERIC(12,2) NOT NULL,
    inss_teto                NUMERIC(12,2) NOT NULL,
    inss_teto_salario        NUMERIC(12,2) NOT NULL,
    ir_deducao_minima        NUMERIC(12,2) NOT NULL,
    ir_limite_isencao        NUMERIC(12,2) NOT NULL,
    ir_limite_reducao        NUMERIC(12,2) NOT NULL,
    ir_reducao_a             NUMERIC(12,5) NOT NULL,
    ir_reducao_b             NUMERIC(12,6) NOT NULL,
    divisor_jornada_6h       INTEGER       NOT NULL,
    divisor_jornada_8h       INTEGER       NOT NULL
);

-- Dados do usuário (v1: uma instalação = um perfil)
CREATE TABLE perfil (
    id               SMALLINT PRIMARY KEY CHECK (id = 1),
    cargo_id         BIGINT        NOT NULL REFERENCES cargo (id),
    delta            INTEGER       NOT NULL,
    dias_sobreaviso  INTEGER       NOT NULL CHECK (dias_sobreaviso BETWEEN 0 AND 31),
    filhos           INTEGER       NOT NULL CHECK (filhos >= 0),
    home_office      BOOLEAN       NOT NULL,
    vr_va            BOOLEAN       NOT NULL,
    saude            VARCHAR(10)   NOT NULL CHECK (saude IN ('NAO', 'SOZINHO', 'DEP')),
    dependentes      INTEGER       NOT NULL CHECK (dependentes >= 0),
    funcef_pct       NUMERIC(5,2)  NOT NULL CHECK (funcef_pct BETWEEN 0 AND 100),
    vale_transporte  BOOLEAN       NOT NULL,
    sindicato        BOOLEAN       NOT NULL,
    saldo_inicial    NUMERIC(14,2) NOT NULL,
    ferias_ativo     BOOLEAN       NOT NULL,
    ferias_mes       SMALLINT      NOT NULL CHECK (ferias_mes BETWEEN 1 AND 12),
    ferias_abono     BOOLEAN       NOT NULL,
    atualizado_em    TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE categoria (
    nome   VARCHAR(60) PRIMARY KEY,
    ordem  INTEGER NOT NULL
);

CREATE TABLE lancamento (
    id           BIGSERIAL PRIMARY KEY,
    descricao    VARCHAR(160)  NOT NULL,
    categoria    VARCHAR(60)   NOT NULL,
    tipo         VARCHAR(12)   NOT NULL CHECK (tipo IN ('RECORRENTE', 'PONTUAL', 'PARCELADO')),
    valor        NUMERIC(14,2) NOT NULL CHECK (valor >= 0),
    mes_inicial  DATE          NOT NULL,        -- sempre dia 1
    parcelas     INTEGER       NOT NULL DEFAULT 0 CHECK (parcelas >= 0),
    criado_em    TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE evento_salarial (
    id          BIGSERIAL PRIMARY KEY,
    tipo        VARCHAR(20)  NOT NULL CHECK (tipo IN ('DECIMO_TERCEIRO')),
    mes         SMALLINT     NOT NULL CHECK (mes BETWEEN 1 AND 12),
    percentual  NUMERIC(5,2) NOT NULL CHECK (percentual > 0 AND percentual <= 100),
    UNIQUE (tipo, mes)
);

CREATE TABLE meta (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(120)  NOT NULL,
    valor_alvo     NUMERIC(14,2) NOT NULL CHECK (valor_alvo >= 0),
    valor_atual    NUMERIC(14,2) NOT NULL CHECK (valor_atual >= 0),
    aporte_mensal  NUMERIC(14,2) NOT NULL CHECK (aporte_mensal >= 0)
);
