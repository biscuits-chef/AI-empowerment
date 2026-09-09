package com.acme.intelligentqa.adapter.out.persistence.mybatis;

import java.sql.Timestamp;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 停止请求、租约领取及状态条件更新的 MyBatis 映射器。 */
@Mapper
public interface AnswerCancellationMapper {

    /**
     * 携带同源凭据并在有界超时内调用 JSON 接口。
     *
     * @param answerId 回答 ID。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param idempotencyKey 幂等键。
     *
     * @param reason 原因。
     *
     * @param requestedAt 请求受理时间。
     *
     * @param messageIdDeadline 等待公司模型消息 ID 的截止时间。
     *
     * @return 接口请求。
     */
    int request(
            @Param("answerId") String answerId,
            @Param("ownerId") String ownerId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("reason") String reason,
            @Param("requestedAt") Timestamp requestedAt,
            @Param("messageIdDeadline") Timestamp messageIdDeadline);

    /**
     * 写入持久化停止任务。
     *
     * @param answerId 回答 ID。
     *
     * @param nextAttemptAt 下一次停止任务计划执行时间。
     *
     * @param now 当前时间。
     *
     * @return 写入持久化停止任务。
     */
    int insertTask(
            @Param("answerId") String answerId,
            @Param("nextAttemptAt") Timestamp nextAttemptAt,
            @Param("now") Timestamp now);

    /**
     * 统计已请求停止但尚未收敛的回答。
     *
     * @param answerId 回答 ID。
     *
     * @return 统计已请求停止但尚未收敛的回答。
     */
    int countCancellationRequested(@Param("answerId") String answerId);

    /**
     * 保存公司模型侧消息 ID。
     *
     * @param answerId 回答 ID。
     *
     * @param providerMessageId 公司模型侧消息 ID。
     *
     * @param dispatchAt 本次任务分发时间。
     *
     * @return 保存公司模型侧消息 ID。
     */
    int recordProviderMessageId(
            @Param("answerId") String answerId,
            @Param("providerMessageId") String providerMessageId,
            @Param("dispatchAt") Timestamp dispatchAt);

    /**
     * 提前唤醒等待模型消息 ID 的停止任务。
     *
     * @param answerId 回答 ID。
     *
     * @param dispatchAt 本次任务分发时间。
     *
     * @return 提前唤醒等待模型消息 ID 的停止任务。
     */
    int wakeTask(@Param("answerId") String answerId, @Param("dispatchAt") Timestamp dispatchAt);

    /**
     * 读取当前可领取的停止任务。
     *
     * @param now 当前时间。
     *
     * @param limit 数量上限。
     *
     * @return 读取当前可领取的停止任务。
     */
    List<String> selectDispatchable(@Param("now") Timestamp now, @Param("limit") int limit);

    /**
     * 将租约过期且已耗尽尝试次数的回答标记为停止失败。
     *
     * @param answerId 回答 ID。
     *
     * @param now 当前时间。
     *
     * @param maximumAttempts 停止接口最大尝试次数。
     *
     * @param errorCode 稳定错误码。
     *
     * @return 更新的回答数量。
     */
    int markAnswerExpiredExhausted(
            @Param("answerId") String answerId,
            @Param("now") Timestamp now,
            @Param("maximumAttempts") int maximumAttempts,
            @Param("errorCode") String errorCode);

    /**
     * 将租约过期且已耗尽尝试次数的任务标记为失败。
     *
     * @param answerId 回答 ID。
     *
     * @param now 当前时间。
     *
     * @param maximumAttempts 停止接口最大尝试次数。
     *
     * @param errorCode 稳定错误码。
     *
     * @return 更新的任务数量。
     */
    int failTaskExpiredExhausted(
            @Param("answerId") String answerId,
            @Param("now") Timestamp now,
            @Param("maximumAttempts") int maximumAttempts,
            @Param("errorCode") String errorCode);

    /**
     * 按租约条件领取待执行的停止任务。
     *
     * @param answerId 回答 ID。
     *
     * @param workerId 当前停止任务执行器标识。
     *
     * @param now 当前时间。
     *
     * @param leaseUntil 租约到期时间。
     *
     * @param maximumAttempts 停止接口最大尝试次数。
     *
     * @return 按租约条件领取待执行的停止任务。
     */
    int claim(
            @Param("answerId") String answerId,
            @Param("workerId") String workerId,
            @Param("now") Timestamp now,
            @Param("leaseUntil") Timestamp leaseUntil,
            @Param("maximumAttempts") int maximumAttempts);

    /**
     * 读取当前租约持有者领取的任务。
     *
     * @param answerId 回答 ID。
     *
     * @param workerId 当前停止任务执行器标识。
     *
     * @return 读取当前租约持有者领取的任务。
     */
    CancellationTaskPersistenceRecord selectClaimed(
            @Param("answerId") String answerId,
            @Param("workerId") String workerId);

    /**
     * 在并发条件允许时将回答标记为已停止。
     *
     * @param answerId 回答 ID。
     *
     * @param workerId 当前停止任务执行器标识。
     *
     * @param cancelledAt 停止完成时间。
     *
     * @return 在并发条件允许时将回答标记为已停止。
     */
    int markCancelled(
            @Param("answerId") String answerId,
            @Param("workerId") String workerId,
            @Param("cancelledAt") Timestamp cancelledAt);

    /**
     * 将停止任务标记为成功完成。
     *
     * @param answerId 回答 ID。
     *
     * @param workerId 当前停止任务执行器标识。
     *
     * @param now 当前时间。
     *
     * @return 将停止任务标记为成功完成。
     */
    int completeTask(
            @Param("answerId") String answerId,
            @Param("workerId") String workerId,
            @Param("now") Timestamp now);

    /**
     * 为暂时失败的停止任务安排下一次执行。
     *
     * @param answerId 回答 ID。
     *
     * @param workerId 当前停止任务执行器标识。
     *
     * @param errorCode 错误码。
     *
     * @param nextAttemptAt 下一次停止任务计划执行时间。
     *
     * @return 为暂时失败的停止任务安排下一次执行。
     */
    int reschedule(
            @Param("answerId") String answerId,
            @Param("workerId") String workerId,
            @Param("errorCode") String errorCode,
            @Param("nextAttemptAt") Timestamp nextAttemptAt);

    /**
     * 保存公司模型停止调用的错误信息。
     *
     * @param answerId 回答 ID。
     *
     * @param errorCode 错误码。
     *
     * @return 保存公司模型停止调用的错误信息。
     */
    int recordAnswerCancellationError(
            @Param("answerId") String answerId,
            @Param("errorCode") String errorCode);

    /**
     * 将生成异常转换为失败或未完整终态。
     *
     * @param answerId 回答 ID。
     *
     * @param workerId 当前停止任务执行器标识。
     *
     * @param errorCode 错误码。
     *
     * @param failedAt 失败记录时间。
     *
     * @return 将生成异常转换为失败或未完整终态。
     */
    int markFailed(
            @Param("answerId") String answerId,
            @Param("workerId") String workerId,
            @Param("errorCode") String errorCode,
            @Param("failedAt") Timestamp failedAt);

    /**
     * 将停止任务标记为重试耗尽或永久失败。
     *
     * @param answerId 回答 ID。
     *
     * @param workerId 当前停止任务执行器标识。
     *
     * @param errorCode 错误码。
     *
     * @param failedAt 失败记录时间。
     *
     * @return 将停止任务标记为重试耗尽或永久失败。
     */
    int failTask(
            @Param("answerId") String answerId,
            @Param("workerId") String workerId,
            @Param("errorCode") String errorCode,
            @Param("failedAt") Timestamp failedAt);
}
