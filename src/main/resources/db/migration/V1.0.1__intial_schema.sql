-- ============= HOUSEHOLDS TABLE =============
CREATE TABLE IF NOT EXISTS public.households (
    id BIGSERIAL PRIMARY KEY,
    household_number VARCHAR(50) UNIQUE NOT NULL,
    primary_address TEXT NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    total_annual_income DECIMAL(15, 2) NOT NULL,
    income_proof_type VARCHAR(50),
    income_proof_url TEXT,
    income_verified_date DATE,
    total_members INTEGER DEFAULT 1,
    household_type VARCHAR(50),
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_households_pincode ON public.households(pincode);
CREATE INDEX IF NOT EXISTS idx_households_city ON public.households(city);

-- ============= BORROWERS TABLE =============
CREATE TABLE IF NOT EXISTS public.borrowers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    phone VARCHAR(15) UNIQUE NOT NULL,
    email VARCHAR(100),
    dob DATE NOT NULL,
    household_id BIGINT REFERENCES public.households(id),
    relationship_to_head VARCHAR(50),
    is_household_head BOOLEAN DEFAULT FALSE,
    individual_annual_income DECIMAL(15, 2),
    occupation VARCHAR(100),
    address TEXT,
    id_proof_type VARCHAR(50) NOT NULL,
    id_proof_number VARCHAR(50) NOT NULL,
    employment_details TEXT,
    income_details TEXT,
    profile_photo_url TEXT,
    credit_score INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_borrowers_phone ON public.borrowers(phone);
CREATE INDEX IF NOT EXISTS idx_borrowers_household ON public.borrowers(household_id);
CREATE INDEX IF NOT EXISTS idx_borrowers_status ON public.borrowers(status);

-- ============= LOAN PRODUCTS TABLE =============
CREATE TABLE IF NOT EXISTS public.loan_products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    min_amount DECIMAL(15, 2) NOT NULL,
    max_amount DECIMAL(15, 2) NOT NULL,
    interest_rate DECIMAL(5, 2) NOT NULL,
    processing_fee_type VARCHAR(20) NOT NULL,
    processing_fee_value DECIMAL(10, 2) NOT NULL,
    tenure_months INTEGER NOT NULL,
    grace_period_days INTEGER DEFAULT 0,
    late_fee_percent DECIMAL(5, 2) DEFAULT 0,
    max_late_fee_percent DECIMAL(5, 2),
    prepayment_charges_type VARCHAR(20),
    prepayment_charges_value DECIMAL(10, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loan_products_status ON public.loan_products(status);

-- ============= LOAN APPLICATIONS TABLE =============
CREATE TABLE IF NOT EXISTS public.loan_applications (
    id BIGSERIAL PRIMARY KEY,
    application_number VARCHAR(50) UNIQUE NOT NULL,
    borrower_id BIGINT NOT NULL REFERENCES public.borrowers(id),
    product_id BIGINT NOT NULL REFERENCES public.loan_products(id),
    requested_amount DECIMAL(15, 2) NOT NULL,
    purpose TEXT,
    preferred_tenure INTEGER,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    approved_amount DECIMAL(15, 2),
    approved_by BIGINT,
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loan_applications_borrower ON public.loan_applications(borrower_id);
CREATE INDEX IF NOT EXISTS idx_loan_applications_status ON public.loan_applications(status);

-- ============= LOANS TABLE =============
CREATE TABLE IF NOT EXISTS public.loans (
    id BIGSERIAL PRIMARY KEY,
    loan_number VARCHAR(50) UNIQUE NOT NULL,
    application_id BIGINT REFERENCES public.loan_applications(id),
    borrower_id BIGINT NOT NULL REFERENCES public.borrowers(id),
    household_id BIGINT REFERENCES public.households(id),
    product_id BIGINT NOT NULL REFERENCES public.loan_products(id),
    principal_amount DECIMAL(15, 2) NOT NULL,
    interest_rate DECIMAL(5, 2) NOT NULL,
    processing_fee DECIMAL(10, 2) DEFAULT 0,
    tenure_months INTEGER NOT NULL,
    repayment_frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    emi_amount DECIMAL(15, 2) NOT NULL,
    total_payable DECIMAL(15, 2) NOT NULL,
    outstanding_principal DECIMAL(15, 2) NOT NULL,
    outstanding_interest DECIMAL(15, 2) NOT NULL,
    total_outstanding DECIMAL(15, 2) NOT NULL,
    total_paid DECIMAL(15, 2) DEFAULT 0,
    disbursement_date DATE NOT NULL,
    disbursement_method VARCHAR(30) NOT NULL,
    first_due_date DATE NOT NULL,
    last_payment_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    closed_date DATE,
    grace_period_days INTEGER DEFAULT 0,
    late_fee_percent DECIMAL(5, 2) DEFAULT 0,
    agreement_url TEXT,
    household_income_at_approval DECIMAL(15, 2),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loans_borrower ON public.loans(borrower_id);
CREATE INDEX IF NOT EXISTS idx_loans_household ON public.loans(household_id);
CREATE INDEX IF NOT EXISTS idx_loans_status ON public.loans(status);
CREATE INDEX IF NOT EXISTS idx_loans_disbursement_date ON public.loans(disbursement_date);

-- ============= REPAYMENT SCHEDULE TABLE =============
CREATE TABLE IF NOT EXISTS public.repayment_schedule (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL REFERENCES public.loans(id),
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    principal_due DECIMAL(15, 2) NOT NULL,
    interest_due DECIMAL(15, 2) NOT NULL,
    total_due DECIMAL(15, 2) NOT NULL,
    principal_paid DECIMAL(15, 2) DEFAULT 0,
    interest_paid DECIMAL(15, 2) DEFAULT 0,
    late_fee_paid DECIMAL(15, 2) DEFAULT 0,
    total_paid DECIMAL(15, 2) DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(loan_id, installment_number)
);

CREATE INDEX IF NOT EXISTS idx_repayment_schedule_loan ON public.repayment_schedule(loan_id);
CREATE INDEX IF NOT EXISTS idx_repayment_schedule_status ON public.repayment_schedule(status);
CREATE INDEX IF NOT EXISTS idx_repayment_schedule_due_date ON public.repayment_schedule(due_date);

-- ============= REPAYMENTS TABLE =============
CREATE TABLE IF NOT EXISTS public.repayments (
    id BIGSERIAL PRIMARY KEY,
    receipt_number VARCHAR(50) UNIQUE NOT NULL,
    loan_id BIGINT NOT NULL REFERENCES public.loans(id),
    borrower_id BIGINT NOT NULL REFERENCES public.borrowers(id),
    household_id BIGINT REFERENCES public.households(id),
    amount DECIMAL(15, 2) NOT NULL,
    principal_paid DECIMAL(15, 2) DEFAULT 0,
    interest_paid DECIMAL(15, 2) DEFAULT 0,
    late_fee_paid DECIMAL(15, 2) DEFAULT 0,
    advance_payment DECIMAL(15, 2) DEFAULT 0,
    payment_date DATE NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    transaction_ref VARCHAR(100),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    receipt_url TEXT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_repayments_loan ON public.repayments(loan_id);
CREATE INDEX IF NOT EXISTS idx_repayments_borrower ON public.repayments(borrower_id);
CREATE INDEX IF NOT EXISTS idx_repayments_payment_date ON public.repayments(payment_date);

-- ============= OVERDUE TRACKING TABLE =============
CREATE TABLE IF NOT EXISTS public.overdue_tracking (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT UNIQUE NOT NULL REFERENCES public.loans(id),
    overdue_since DATE NOT NULL,
    overdue_days INTEGER NOT NULL,
    overdue_principal DECIMAL(15, 2) NOT NULL,
    overdue_interest DECIMAL(15, 2) NOT NULL,
    overdue_amount DECIMAL(15, 2) NOT NULL,
    penalty_amount DECIMAL(15, 2) DEFAULT 0,
    total_due DECIMAL(15, 2) NOT NULL,
    last_checked_at TIMESTAMP NOT NULL,
    collection_stage VARCHAR(30) NOT NULL DEFAULT 'SOFT_REMINDER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_overdue_tracking_loan ON public.overdue_tracking(loan_id);
CREATE INDEX IF NOT EXISTS idx_overdue_tracking_stage ON public.overdue_tracking(collection_stage);
CREATE INDEX IF NOT EXISTS idx_overdue_tracking_days ON public.overdue_tracking(overdue_days);

-- ============= COLLECTION ACTIVITIES TABLE =============
CREATE TABLE IF NOT EXISTS public.collection_activities (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL REFERENCES public.loans(id),
    activity_type VARCHAR(50) NOT NULL,
    contact_method VARCHAR(30) NOT NULL,
    borrower_response TEXT,
    promise_to_pay_date DATE,
    payment_arrangement TEXT,
    notes TEXT,
    assigned_to BIGINT,
    activity_date TIMESTAMP NOT NULL,
    next_follow_up_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_collection_activities_loan ON public.collection_activities(loan_id);
CREATE INDEX IF NOT EXISTS idx_collection_activities_date ON public.collection_activities(activity_date);