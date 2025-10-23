-- 1. borrowers
CREATE TABLE IF NOT EXISTS public.borrowers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    dob DATE NOT NULL,
    address TEXT,
    id_proof_type VARCHAR(50),
    id_proof_number VARCHAR(100) UNIQUE,
    status VARCHAR(50) DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. admin_users
CREATE TABLE IF NOT EXISTS public.admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'active',
    last_login TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. loan_products
CREATE TABLE IF NOT EXISTS public.loan_products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    min_amount NUMERIC(15,2) NOT NULL,
    max_amount NUMERIC(15,2) NOT NULL,
    interest_rate NUMERIC(5,2) NOT NULL,
    processing_fee NUMERIC(5,2),
    tenure_months INT NOT NULL,
    grace_period_days INT DEFAULT 0,
    late_fee_percent NUMERIC(5,2),
    status VARCHAR(50) DEFAULT 'active'
);

-- 4. loan_applications
CREATE TABLE IF NOT EXISTS public.loan_applications (
    id BIGSERIAL PRIMARY KEY,
    borrower_id BIGINT REFERENCES public.borrowers(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES public.loan_products(id),
    requested_amount NUMERIC(15,2) NOT NULL,
    purpose TEXT,
    status VARCHAR(50) DEFAULT 'pending',
    approved_by BIGINT REFERENCES public.admin_users(id),
    approved_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. loans
CREATE TABLE IF NOT EXISTS public.loans (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT REFERENCES public.loan_applications(id) ON DELETE CASCADE,
    borrower_id BIGINT REFERENCES public.borrowers(id),
    product_id BIGINT REFERENCES public.loan_products(id),
    principal_amount NUMERIC(15,2) NOT NULL,
    interest_rate NUMERIC(5,2) NOT NULL,
    processing_fee NUMERIC(5,2),
    tenure_months INT NOT NULL,
    emi_amount NUMERIC(15,2),
    total_payable NUMERIC(15,2),
    disbursement_date DATE,
    first_due_date DATE,
    status VARCHAR(50) DEFAULT 'active',
    closed_date DATE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. repayment_schedule
CREATE TABLE IF NOT EXISTS public.repayment_schedule (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT REFERENCES public.loans(id) ON DELETE CASCADE,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    principal_due NUMERIC(15,2),
    interest_due NUMERIC(15,2),
    total_due NUMERIC(15,2),
    principal_paid NUMERIC(15,2) DEFAULT 0,
    interest_paid NUMERIC(15,2) DEFAULT 0,
    total_paid NUMERIC(15,2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'pending'
);

-- 7. repayments
CREATE TABLE IF NOT EXISTS public.repayments (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT REFERENCES public.loans(id) ON DELETE CASCADE,
    amount NUMERIC(15,2) NOT NULL,
    payment_date TIMESTAMPTZ NOT NULL,
    payment_method VARCHAR(50),
    transaction_ref VARCHAR(100) UNIQUE,
    created_by BIGINT REFERENCES public.admin_users(id),
    notes TEXT,
    status VARCHAR(50) DEFAULT 'completed',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 8. overdue_tracking
CREATE TABLE IF NOT EXISTS public.overdue_tracking (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT REFERENCES public.loans(id) ON DELETE CASCADE,
    overdue_since DATE,
    overdue_days INT,
    penalty_amount NUMERIC(15,2),
    last_checked_at TIMESTAMPTZ DEFAULT NOW()
);

-- 9. collection_activities
CREATE TABLE IF NOT EXISTS public.collection_activities (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT REFERENCES public.loans(id) ON DELETE CASCADE,
    activity_type VARCHAR(100),
    contact_method VARCHAR(100),
    notes TEXT,
    assigned_to BIGINT REFERENCES public.admin_users(id),
    activity_date DATE,
    next_follow_up_date DATE
);

-- 10. notifications
CREATE TABLE IF NOT EXISTS public.notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    recipient_type VARCHAR(50) NOT NULL,  -- borrower/admin
    channel VARCHAR(50) NOT NULL,         -- sms/email
    template_id BIGINT,
    content TEXT,
    status VARCHAR(50) DEFAULT 'pending',
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ
);
