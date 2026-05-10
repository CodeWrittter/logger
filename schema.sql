CREATE TABLE call_logs (
    id BIGSERIAL PRIMARY KEY,
    number TEXT,
    saved_name TEXT,
    call_type TEXT,
    duration_seconds INTEGER,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE sms_logs (
    id BIGSERIAL PRIMARY KEY,
    sender TEXT,
    saved_name TEXT,
    content TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    package_name TEXT,
    app_name TEXT,
    title TEXT,
    content TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE location_logs (
    id BIGSERIAL PRIMARY KEY,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    accuracy DOUBLE PRECISION,
    position_status TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE system_event_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type TEXT,
    detail TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE screen_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE battery_logs (
    id BIGSERIAL PRIMARY KEY,
    level INTEGER,
    is_charging BOOLEAN,
    event_type TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE app_usage_logs (
    id BIGSERIAL PRIMARY KEY,
    package_name TEXT,
    app_name TEXT,
    usage_duration_seconds INTEGER,
    window_start BIGINT,
    window_end BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE network_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type TEXT,
    detail TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE error_logs (
    id BIGSERIAL PRIMARY KEY,
    error_type TEXT,
    message TEXT,
    stacktrace TEXT,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Disable RLS on all tables (personal use only)
ALTER TABLE call_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE sms_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE notification_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE location_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE system_event_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE screen_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE battery_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE app_usage_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE network_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE error_logs DISABLE ROW LEVEL SECURITY;
