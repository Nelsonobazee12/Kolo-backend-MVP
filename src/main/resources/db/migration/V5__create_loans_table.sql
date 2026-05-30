CREATE TABLE loans (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       borrower_id UUID NOT NULL REFERENCES users(id),
                       group_id UUID NOT NULL REFERENCES groups(id),
                       membership_id UUID NOT NULL REFERENCES memberships(id),
                       guarantor_id UUID REFERENCES users(id),
                       amount_requested BIGINT NOT NULL,
                       amount_approved BIGINT,
                       interest_rate DECIMAL(5,4) NOT NULL,
                       total_repayable BIGINT,
                       amount_repaid BIGINT DEFAULT 0,
                       collateral_type VARCHAR(30),
                       collateral_document_url VARCHAR(500),
                       tier VARCHAR(10) NOT NULL,
                       status VARCHAR(20) DEFAULT 'PENDING',
                       rejection_reason VARCHAR(255),
                       due_date DATE,
                       created_at TIMESTAMP DEFAULT NOW(),
                       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE loan_votes (
                            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                            loan_id UUID NOT NULL REFERENCES loans(id),
                            voter_id UUID NOT NULL REFERENCES users(id),
                            vote VARCHAR(10) NOT NULL,
                            reason VARCHAR(255),
                            created_at TIMESTAMP DEFAULT NOW(),
                            UNIQUE(loan_id, voter_id)
);

CREATE INDEX idx_loans_borrower ON loans(borrower_id);
CREATE INDEX idx_loans_group ON loans(group_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loan_votes_loan ON loan_votes(loan_id);