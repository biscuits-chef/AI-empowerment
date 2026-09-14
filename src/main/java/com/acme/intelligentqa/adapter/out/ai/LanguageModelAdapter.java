package com.acme.intelligentqa.adapter.out.ai;

import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.BusinessFact;
import com.acme.intelligentqa.domain.port.out.LanguageModelPort;
import com.acme.intelligentqa.domain.port.out.LanguageModelCancellationPort;
import com.acme.intelligentqa.infrastructure.companymodel.CompanyModelApiClient;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * 在演示模型与公司 HiAgent 客户端之间路由生成及停止请求。
 */
@Component
public class LanguageModelAdapter implements LanguageModelPort, LanguageModelCancellationPort {

    /**
     * 配置参数。
     */
    private final QaProperties properties;
    /**
     * 公司 HiAgent 接口客户端。
     */
    private final CompanyModelApiClient companyModelApiClient;

    /**
     * 创建 {@code LanguageModelAdapter} 实例。
     *
     * @param properties 配置参数。
     *
     * @param companyModelApiClient 公司 HiAgent 接口客户端。
     */
    public LanguageModelAdapter(
            final QaProperties properties,
            final CompanyModelApiClient companyModelApiClient) {
        this.properties = properties;
        this.companyModelApiClient = companyModelApiClient;
    }

    /**
     * 编排证据获取并流式生成回答。
     *
     * @param request 接口请求。
     *
     * @param chunkConsumer 回答文本增量消费回调。
     *
     * @return 编排证据获取并流式生成回答。
     */
    @Override
    public GenerationResult generate(final GenerationRequest request, final Consumer<String> chunkConsumer) {
        return generate(request, chunkConsumer, LanguageModelPort.NO_CANCELLATION);
    }

    /**
     * 编排证据获取并流式生成回答。
     *
     * @param request 接口请求。
     *
     * @param chunkConsumer 回答文本增量消费回调。
     *
     * @param control 生成取消状态控制器。
     *
     * @return 编排证据获取并流式生成回答。
     */
    @Override
    public GenerationResult generate(
            final GenerationRequest request,
            final Consumer<String> chunkConsumer,
            final GenerationControl control) {
        if (!properties.demoMode()) {
            return companyModelApiClient.generate(request, chunkConsumer, control);
        }
        if (request.businessFacts().isEmpty()) {
            chunkConsumer.accept("未查询到可核验的本地业务数据。");
        } else {
            chunkConsumer.accept("数据库查询结果：\n");
            for (final BusinessFact fact : request.businessFacts()) {
                chunkConsumer.accept("- " + fact.content() + "\n");
            }
            chunkConsumer.accept("以上为开发环境模拟回答，正式环境将由公司大模型依据证据整理。");
        }
        return new GenerationResult("demo-model", "stop");
    }

    /**
     * 请求公司模型停止远端生成。
     *
     * @param ownerId 用户所有者 ID。
     *
     * @param messageId 公司模型侧消息 ID。
     */
    @Override
    public void stop(final String ownerId, final String messageId) {
        if (!properties.demoMode()) {
            companyModelApiClient.stopMessage(ownerId, messageId);
        }
    }
}
