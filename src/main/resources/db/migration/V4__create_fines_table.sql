CREATE TABLE fines (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       membership_id UUID NOT NULL REFERENCES memberships(id),
                       group_id UUID NOT NULL REFERENCES groups(id),
                       user_id UUID NOT NULL REFERENCES users(id),
                       amount BIGINT NOT NULL,
                       reason VARCHAR(255),
                       days_late INTEGER DEFAULT 0,
                       platform_cut BIGINT DEFAULT 0,
                       pool_cut BIGINT DEFAULT 0,
                       status VARCHAR(20) DEFAULT 'PENDING',
                       created_at TIMESTAMP DEFAULT NOW(),
                       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_fines_membership ON fines(membership_id);
CREATE INDEX idx_fines_user ON fines(user_id);
CREATE INDEX idx_fines_group ON fines(group_id);
CREATE INDEX idx_fines_status ON fines(status);