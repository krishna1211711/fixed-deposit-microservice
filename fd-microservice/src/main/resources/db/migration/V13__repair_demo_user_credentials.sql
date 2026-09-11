-- Ensures existing lab databases created from the original seed use the documented demo password.
UPDATE users
SET password_hash = '$2a$10$GaSHRmdRQnLE2F/uLgweMefkQL8cEfhvLwdOZKmpG5e3UA6.INp/W'
WHERE username IN ('admin', 'officer1', 'johndoe');
