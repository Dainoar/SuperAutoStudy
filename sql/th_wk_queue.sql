
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for th_wk_queue
-- ----------------------------
DROP TABLE IF EXISTS `th_wk_queue`;
CREATE TABLE `th_wk_queue`  (
  `id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '任务id',
  `login_account` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '登陆账号',
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码',
  `course_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '课程id',
  `course_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '课程名称',
  `priority` int(11) NULL DEFAULT NULL COMMENT '优先级',
  `status` int(11) NULL DEFAULT NULL COMMENT '0-待处理，1-进行中，2-队列中，3-考试中，4-已完成，5-异常, 6-暂停',
  `retry_count` int(11) NULL DEFAULT 0 COMMENT '重试次数',
  `creat_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `machine_num` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所处机器编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_th_wk_queue_account_course` (`login_account`, `course_id`)
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '网课任务队列表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
