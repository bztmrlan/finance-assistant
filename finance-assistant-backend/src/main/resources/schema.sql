CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_email ON users(email);

CREATE TABLE categories (
    category_id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER'))
);

CREATE INDEX idx_category_type ON categories(type);

CREATE TABLE transactions (
    transaction_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(category_id),
    amount DECIMAL(19,4) NOT NULL CHECK (amount != 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    description TEXT,
    date DATE NOT NULL
);

CREATE INDEX idx_transaction_user_date ON transactions(user_id, date);
CREATE INDEX idx_transaction_category ON transactions(category_id);

CREATE TABLE budgets (
    budget_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'UPCOMING')),
    CHECK (end_date > start_date)
);

CREATE INDEX idx_budget_user ON budgets(user_id);
CREATE INDEX idx_budget_dates ON budgets(start_date, end_date);

CREATE TABLE budget_categories (
    id UUID PRIMARY KEY,
    budget_id UUID NOT NULL REFERENCES budgets(budget_id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(category_id),
    limit_amount DECIMAL(19,4) NOT NULL,
    spent_amount DECIMAL(19,4) NOT NULL DEFAULT 0
);

CREATE INDEX idx_budget_category ON budget_categories(category_id);

CREATE TABLE rules (
    rule_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(category_id),
    name VARCHAR(100) NOT NULL,
    condition_type VARCHAR(20) NOT NULL
        CHECK (condition_type IN ('LESS_THAN', 'GREATER_THAN', 'EQUAL_TO')),
    threshold DECIMAL(19,4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    period VARCHAR(20) NOT NULL
        CHECK (period IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY'))
);

CREATE INDEX idx_rule_user ON rules(user_id);

CREATE TABLE goals (
    goal_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(category_id),
    name VARCHAR(100) NOT NULL,
    target_amount DECIMAL(19,4) NOT NULL,
    current_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    target_date DATE NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_goal_user ON goals(user_id);


CREATE TABLE alerts (
    alert_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    source_type VARCHAR(20) NOT NULL
        CHECK (source_type IN ('RULE', 'BUDGET', 'GOAL')),
    source_id UUID NOT NULL, -- Polymorphic reference
    message TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_user ON alerts(user_id);



CREATE TABLE insights (
    insight_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL
        CHECK (type IN ('SPENDING_TREND', 'SAVINGS_OPPORTUNITY', 'UNUSUAL_ACTIVITY', 'BUDGET_REVIEW')),
    message TEXT NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    viewed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_insight_user ON insights(user_id);
CREATE INDEX idx_insight_type ON insights(type);

CREATE TABLE user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC'
);

CREATE INDEX idx_user_settings_user ON user_settings(user_id);