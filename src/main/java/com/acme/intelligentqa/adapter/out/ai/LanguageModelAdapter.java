package com.acme.intelligentqa.adapter.out.ai;

import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.port.out.LanguageModelCancellationPort;
import com.acme.intelligentqa.domain.port.out.LanguageModelPort;
import com.acme.intelligentqa.infrastructure.companymodel.CompanyModelApiClient;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
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
        // ==================== 固定模拟返回（最小化改动，方便注释与验证） ====================
        // 验证完成后或接入真实外部大模型服务时，注释掉本模拟段并恢复下方原始调用逻辑：
        final String[] thinkingChunks = {
                "<think>\n",
                "收到用户业务咨询，开始分析问题语义与业务意图...\n",
                "1. 意图识别：识别为理财产品基础信息与交易规则查询。\n",
                "2. 知识库检索：已检索到《浦银理财产品管理规范_2024版》与《业务办理操作指引》。\n",
                "3. 业务数据库查询：已通过受控通道查询产品要素表（PROD_INFO）及申购开放状态。\n",
                "4. 双通道交叉比对：知识库与数据库中产品起售金额、支持渠道与到期规则完全一致。\n",
                "5. 综合生成策略：组织标准答复，明确办理渠道、材料准备清单及系统流转节点。\n",
                "</think>\n\n"
        };
        for (final String chunk : thinkingChunks) {
            if (control.isCancellationRequested()) {
                throw new com.acme.intelligentqa.common.error.GenerationCancelledException();
            }
            chunkConsumer.accept(chunk);
            LockSupport.parkNanos(120_000_000L);
        }

        final String[] answerChunks = {
                "根据浦银理财最新业务规范与管理规定，",
                "您所咨询的理财产品与业务现已支持全流程线上办理。\n\n",
                "### 办理要点与指引：\n",
                "1. **适用渠道**：统一协同门户【业务办理】模块或手机银行企业版。\n",
                "2. **所需材料**：法定代表人或经办人有效身份证件、业务办理授权书及机构账户信息。\n",
                "3. **审核周期**：线上提交申请后，系统预计在 1 个工作日内完成合规与资质校验，审核通过后即刻生效。\n\n",
                "如有任何疑问，可参考下方推荐问题进一步咨询。"
        };
        for (final String chunk : answerChunks) {
            if (control.isCancellationRequested()) {
                throw new com.acme.intelligentqa.common.error.GenerationCancelledException();
            }
            chunkConsumer.accept(chunk);
            LockSupport.parkNanos(100_000_000L);
        }

        final Map<String, Object> extraFields = new LinkedHashMap<>();
        extraFields.put("rewrite_query", "浦银理财产品线上办理流程及所需材料要求");
        extraFields.put("doc_reference_json",
                "[{\"title\":\"浦银理财产品管理规范_2024版.pdf\",\"page\":12},"
                        + "{\"title\":\"理财产品业务办理操作指引.pdf\",\"page\":5}]");
        extraFields.put("sql_message",
                "SELECT prod_code, prod_name, min_amount, status FROM prod_info "
                        + "WHERE prod_status = 'OPEN' AND channel = 'ONLINE'");
        extraFields.put("scene_name", "理财产品信息问答");

        return new GenerationResult(
                "mock-model",
                "stop",
                Arrays.asList("如何在线办理该理财业务？", "办理该业务需要准备哪些申请材料？", "业务办理的审核周期大概是多久？"),
                1280,
                extraFields);
        /*
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
        */
        // =========================================================================
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
