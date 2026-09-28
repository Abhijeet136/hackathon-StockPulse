CREATE TABLE product (
    id                 BIGSERIAL PRIMARY KEY,
    sku                VARCHAR(32)    NOT NULL UNIQUE,
    name               VARCHAR(200)   NOT NULL,
    category           VARCHAR(32)    NOT NULL,
    current_price      NUMERIC(12, 2) NOT NULL CHECK (current_price > 0),
    stock_level        INTEGER        NOT NULL CHECK (stock_level >= 0),
    reorder_threshold  INTEGER        NOT NULL CHECK (reorder_threshold >= 0),
    demand_velocity    INTEGER        NOT NULL DEFAULT 0 CHECK (demand_velocity >= 0),
    status             VARCHAR(32)    NOT NULL,
    cost_price         NUMERIC(12, 2),
    supplier_id        VARCHAR(64),
    version            BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_product_category ON product (category);
CREATE INDEX idx_product_status ON product (status);

CREATE TABLE pricing_suggestion (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT         NOT NULL REFERENCES product (id),
    current_price     NUMERIC(12, 2) NOT NULL,
    recommended_price NUMERIC(12, 2) NOT NULL,
    direction         VARCHAR(16)    NOT NULL,
    confidence        NUMERIC(4, 3)  NOT NULL,
    reasoning         TEXT           NOT NULL,
    status            VARCHAR(16)    NOT NULL,
    trigger_reason    VARCHAR(32)    NOT NULL,
    source            VARCHAR(32)    NOT NULL,
    dedupe_key        VARCHAR(128) UNIQUE,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_pricing_suggestion_status ON pricing_suggestion (status);

CREATE TABLE reorder_suggestion (
    id                   BIGSERIAL PRIMARY KEY,
    product_id           BIGINT        NOT NULL REFERENCES product (id),
    current_stock        INTEGER       NOT NULL,
    recommended_quantity INTEGER       NOT NULL CHECK (recommended_quantity > 0),
    lead_time_days       INTEGER       NOT NULL,
    confidence           NUMERIC(4, 3) NOT NULL,
    reasoning            TEXT          NOT NULL,
    status               VARCHAR(16)   NOT NULL,
    trigger_reason       VARCHAR(32)   NOT NULL,
    source               VARCHAR(32)   NOT NULL,
    dedupe_key           VARCHAR(128) UNIQUE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_reorder_suggestion_status ON reorder_suggestion (status);
