CREATE TABLE groups (
                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        name VARCHAR(100) NOT NULL,
                        description VARCHAR(255),
                        created_by UUID NOT NULL REFERENCES users(id),
                        paystack_virtual_account_number VARCHAR(20),
                        paystack_bank_name VARCHAR(50),
                        pool_balance BIGINT DEFAULT 0,
                        total_loan_book BIGINT DEFAULT 0,
                        max_members INTEGER DEFAULT 20,
                        is_premium BOOLEAN DEFAULT FALSE,
                        status VARCHAR(20) DEFAULT 'ACTIVE',
                        created_at TIMESTAMP DEFAULT NOW(),
                        updated_at TIMESTAMP DEFAULT NOW(),
                        deleted_at TIMESTAMP
);

CREATE TABLE memberships (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             user_id UUID NOT NULL REFERENCES users(id),
                             group_id UUID NOT NULL REFERENCES groups(id),
                             contribution_amount BIGINT NOT NULL,
                             frequency VARCHAR(10) NOT NULL,
                             next_due_date DATE,
                             savings_balance BIGINT DEFAULT 0,
                             role VARCHAR(10) DEFAULT 'MEMBER',
                             grace_period_used BOOLEAN DEFAULT FALSE,
                             status VARCHAR(20) DEFAULT 'ACTIVE',
                             joined_at TIMESTAMP DEFAULT NOW(),
                             updated_at TIMESTAMP DEFAULT NOW(),
                             UNIQUE(user_id, group_id)
);

CREATE TABLE invitations (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             group_id UUID NOT NULL REFERENCES groups(id),
                             invited_by UUID NOT NULL REFERENCES users(id),
                             phone_number VARCHAR(15) NOT NULL,
                             token VARCHAR(100) UNIQUE NOT NULL,
                             status VARCHAR(20) DEFAULT 'PENDING',
                             expires_at TIMESTAMP NOT NULL,
                             created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_memberships_user ON memberships(user_id);
CREATE INDEX idx_memberships_group ON memberships(group_id);
CREATE INDEX idx_invitations_token ON invitations(token);
CREATE INDEX idx_invitations_phone ON invitations(phone_number);