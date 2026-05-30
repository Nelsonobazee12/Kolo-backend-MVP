CREATE TABLE transactions (
                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              user_id UUID NOT NULL REFERENCES users(id),
                              group_id UUID NOT NULL REFERENCES groups(id),
                              membership_id UUID REFERENCES memberships(id),
                              type VARCHAR(30) NOT NULL,
                              amount BIGINT NOT NULL,
                              paystack_reference VARCHAR(100) UNIQUE,
                              paystack_status VARCHAR(20),
                              status VARCHAR(20) DEFAULT 'PENDING',
                              metadata TEXT,
                              created_at TIMESTAMP DEFAULT NOW(),
                              updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_transactions_user ON transactions(user_id);
CREATE INDEX idx_transactions_group ON transactions(group_id);
CREATE INDEX idx_transactions_reference ON transactions(paystack_reference);
CREATE INDEX idx_transactions_type ON transactions(type);