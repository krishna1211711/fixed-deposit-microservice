-- V25 was introduced with UTC-oriented containers. Normalize only an untouched
-- singleton that still equals the host UTC date at migration time. Once batches
-- or time travel advance the row, the application-owned Banking Clock is authoritative.
UPDATE fd_business_date
   SET business_date = DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 330 MINUTE)),
       updated_by = 'V26_BANKING_TIME_ZONE',
       updated_at = UTC_TIMESTAMP(6),
       version = version + 1
 WHERE singleton_id = 1
   AND business_date = UTC_DATE()
   AND business_date <> DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 330 MINUTE));
