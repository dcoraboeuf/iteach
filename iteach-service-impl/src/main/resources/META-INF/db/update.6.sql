-- Set the verified flag to true for existing users

ALTER TABLE USERS ADD COLUMN VERIFIED BOOLEAN;
UPDATE USERS SET VERIFIED = TRUE;
ALTER TABLE USERS ALTER COLUMN VERIFIED SET NOT NULL;

-- @rollback
ALTER TABLE USERS DROP COLUMN IF EXISTS VERIFIED;

-- @mysql

ALTER TABLE USERS ADD COLUMN VERIFIED BOOLEAN;
UPDATE USERS SET VERIFIED = TRUE;
ALTER TABLE USERS MODIFY COLUMN VERIFIED BOOLEAN NOT NULL;

-- @mysql-rollback
ALTER TABLE USERS DROP COLUMN VERIFIED;
