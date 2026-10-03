-- Customer and product identifiers cross service boundaries. The FD service stores
-- immutable booked terms and identifiers, but does not enforce another service's
-- lifecycle with database foreign keys. Internal FD aggregate foreign keys remain.
SET @customer_fk = (
    SELECT constraint_name
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'fd_accounts'
      AND referenced_table_name = 'customer_profile'
    LIMIT 1
);
SET @drop_customer_fk = IF(
    @customer_fk IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE fd_accounts DROP FOREIGN KEY `', @customer_fk, '`')
);
PREPARE drop_customer_statement FROM @drop_customer_fk;
EXECUTE drop_customer_statement;
DEALLOCATE PREPARE drop_customer_statement;

SET @product_fk = (
    SELECT constraint_name
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'fd_accounts'
      AND referenced_table_name = 'products'
    LIMIT 1
);
SET @drop_product_fk = IF(
    @product_fk IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE fd_accounts DROP FOREIGN KEY `', @product_fk, '`')
);
PREPARE drop_product_statement FROM @drop_product_fk;
EXECUTE drop_product_statement;
DEALLOCATE PREPARE drop_product_statement;
