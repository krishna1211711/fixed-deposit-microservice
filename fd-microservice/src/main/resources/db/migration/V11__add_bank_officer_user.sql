-- Add BANK_OFFICER seed user
-- Username: officer1, Password: admin123 (same bcrypt hash used in V10 for consistency)
INSERT INTO users (username, password_hash, email, role)
VALUES ('officer1', '$2a$10$X/9zGk.q/6cZ0rU.sHlM8O1W8d2I2M7z.C5.N7l0d/G8J3P.5X9yW', 'officer@bank.com', 'BANK_OFFICER');
