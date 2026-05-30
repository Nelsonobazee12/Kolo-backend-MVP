CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       phone_number VARCHAR(15) UNIQUE NOT NULL,
                       full_name VARCHAR(100),
                       pin_hash VARCHAR(255),
                       bvn_hash VARCHAR(255),
                       bvn_verified BOOLEAN DEFAULT FALSE,
                       trust_score INTEGER DEFAULT 50,
                       fine_count INTEGER DEFAULT 0,
                       total_cycles_completed INTEGER DEFAULT 0,
                       kyc_tier VARCHAR(20) DEFAULT 'BRONZE',
                       status VARCHAR(20) DEFAULT 'ACTIVE',
                       is_blacklisted BOOLEAN DEFAULT FALSE,
                       refresh_token_hash VARCHAR(255),
                       created_at TIMESTAMP DEFAULT NOW(),
                       updated_at TIMESTAMP DEFAULT NOW(),
                       deleted_at TIMESTAMP
);

CREATE INDEX idx_users_phone ON users(phone_number);
CREATE INDEX idx_users_bvn ON users(bvn_hash);