-- Apply this migration to existing installations after resolving duplicate tasks.
ALTER TABLE th_wk_queue MODIFY COLUMN id varchar(36) NOT NULL COMMENT '任务id';
ALTER TABLE th_wk_queue ADD UNIQUE KEY uk_th_wk_queue_account_course (login_account, course_id);
ALTER TABLE th_wk_user ADD COLUMN id bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE th_wk_user ADD UNIQUE KEY uk_th_wk_user_account (account);
