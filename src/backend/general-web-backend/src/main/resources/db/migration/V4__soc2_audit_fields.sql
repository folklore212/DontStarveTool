ALTER TABLE audit_logs ADD COLUMN session_id VARCHAR(64) AFTER user_agent;
ALTER TABLE audit_logs ADD COLUMN request_id VARCHAR(64) AFTER session_id;
ALTER TABLE audit_logs ADD COLUMN client_ip_chain VARCHAR(512) AFTER request_id;
