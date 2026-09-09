package com.acme.intelligentqa.application.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 提供应用服务共享的输入校验与轻量辅助逻辑。
 */
final class ApplicationSupport {

    /**
     * 请求幂等头名称。
     */
    private static final String IDEMPOTENCY_KEY = "idempotencyKey";

    /**
     * 创建 {@code ApplicationSupport} 实例。
     */
    private ApplicationSupport() {
    }

    /**
     * 校验文本非空并返回原值。
     *
     * @param value 输入值。
     *
     * @param field 字段名称。
     *
     * @return 校验文本非空并返回原值。
     */
    static String requireText(final String value, final String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (IDEMPOTENCY_KEY.equals(field) && value.length() > 128) {
            throw new IllegalArgumentException("idempotencyKey must not exceed 128 characters");
        }
        return value;
    }

    /**
     * 确保生成投递发生在当前事务提交之后。
     *
     * @param task 待执行的停止任务。
     */
    static void runAfterCommit(final Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new AfterCommitSynchronization(task));
        } else {
            task.run();
        }
    }

    /**
     * 数据库事务提交后的异步工作回调。
     */
    private static final class AfterCommitSynchronization implements TransactionSynchronization {
        /**
         * 待执行的停止任务。
         */
        private final Runnable task;

        /**
         * 创建 {@code AfterCommitSynchronization} 实例。
         *
         * @param task 待执行的停止任务。
         */
        AfterCommitSynchronization(final Runnable task) {
            this.task = task;
        }

        /**
         * 在数据库事务提交后启动异步工作。
         */
        @Override
        public void afterCommit() {
            task.run();
        }
    }
}
