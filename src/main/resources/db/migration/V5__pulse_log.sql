CREATE TABLE pulse_log (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    num_unidad      VARCHAR(100)    NOT NULL,
    status          ENUM(
                        'SENT',
                        'SKIPPED_INACTIVE',
                        'SKIPPED_OUT_OF_WINDOW',
                        'SKIPPED_STALE',
                        'SKIPPED_NO_COORDS',
                        'REJECTED',
                        'ERROR'
                    )               NOT NULL,
    lat             DECIMAL(9,6)    NULL,
    lon             DECIMAL(9,6)    NULL,
    provider        VARCHAR(50)     NULL,
    tracking_number VARCHAR(255)    NULL,
    sent_at         DATETIME        NOT NULL,
    error_message   VARCHAR(500)    NULL,
    INDEX idx_pulse_log_num_unidad  (num_unidad),
    INDEX idx_pulse_log_status      (status),
    INDEX idx_pulse_log_sent_at     (sent_at),
    INDEX idx_pulse_log_unit_sent   (num_unidad, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
