package com.acme.intelligentqa.adapter.out.business;

import com.acme.intelligentqa.common.error.DependencyUnavailableException;
import com.acme.intelligentqa.config.BusinessQueryProperties;
import com.acme.intelligentqa.config.QaProperties;
import com.acme.intelligentqa.domain.model.BusinessFact;
import com.acme.intelligentqa.domain.model.BusinessQueryPlan;
import com.acme.intelligentqa.domain.model.BusinessSemanticQuery;
import com.acme.intelligentqa.domain.model.CompiledBusinessQuery;
import com.acme.intelligentqa.domain.model.QueryIntent;
import com.acme.intelligentqa.domain.port.out.BusinessDataQueryPort;
import com.acme.intelligentqa.domain.service.PhaseOneBusinessQueryPlanner;
import com.acme.intelligentqa.domain.service.PhaseOneBusinessSemanticParser;
import com.acme.intelligentqa.domain.service.PhaseOneBusinessSqlCompiler;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * 编排一期语义解析、查询规划、受控 SQL 编译和 MyBatis 数据执行的业务查询适配器。
 */
@Component
public class BusinessDataAdapter implements BusinessDataQueryPort {

    /** 业务日期统一使用的公司时区。 */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    /** 标准字段对应的物理结果别名。 */
    private static final Map<BusinessSemanticQuery.Field, String> COLUMN_NAMES;
    /** 标准字段对应的中文业务名称。 */
    private static final Map<BusinessSemanticQuery.Field, String> DISPLAY_NAMES;
    /** 当前已由批准物理视图实际返回的标准字段。 */
    private static final java.util.Set<BusinessSemanticQuery.Field> PHYSICALLY_MAPPED_FIELDS =
            Collections.unmodifiableSet(EnumSet.of(
                    BusinessSemanticQuery.Field.PRODUCT_CODE,
                    BusinessSemanticQuery.Field.PRODUCT_NAME,
                    BusinessSemanticQuery.Field.PRODUCT_MANAGER,
                    BusinessSemanticQuery.Field.REGULATORY_INVESTMENT_MANAGER,
                    BusinessSemanticQuery.Field.DEPARTMENT_INVESTMENT_MANAGER,
                    BusinessSemanticQuery.Field.PRODUCT_FORM,
                    BusinessSemanticQuery.Field.END_DATE,
                    BusinessSemanticQuery.Field.TRADE_SERIAL_NUMBER,
                    BusinessSemanticQuery.Field.TRADER_NAME,
                    BusinessSemanticQuery.Field.TRADE_DATE,
                    BusinessSemanticQuery.Field.PRODUCT_MATURITY_DATE,
                    BusinessSemanticQuery.Field.PERIODIC_OPEN_BASE_DATE,
                    BusinessSemanticQuery.Field.SNAPSHOT_DATE));

    static {
        final Map<BusinessSemanticQuery.Field, String> columns = new EnumMap<>(BusinessSemanticQuery.Field.class);
        final Map<BusinessSemanticQuery.Field, String> displays = new EnumMap<>(BusinessSemanticQuery.Field.class);
        register(columns, displays, BusinessSemanticQuery.Field.PRODUCT_CODE, "productCode", "产品代码");
        register(columns, displays, BusinessSemanticQuery.Field.PRODUCT_NAME, "productName", "产品名称");
        register(columns, displays, BusinessSemanticQuery.Field.PRODUCT_MANAGER, "productManager", "产品经理");
        register(columns, displays, BusinessSemanticQuery.Field.INVESTMENT_MANAGER, "investmentManager", "投资经理");
        register(columns, displays, BusinessSemanticQuery.Field.REGULATORY_INVESTMENT_MANAGER,
                "regulatoryInvestmentManager", "监管口径投资经理");
        register(columns, displays, BusinessSemanticQuery.Field.DEPARTMENT_INVESTMENT_MANAGER,
                "departmentInvestmentManager", "产品部口径投资经理");
        register(columns, displays, BusinessSemanticQuery.Field.PRODUCT_FORM, "productForm", "产品形式");
        register(columns, displays, BusinessSemanticQuery.Field.PRODUCT_STATUS, "productStatus", "产品状态");
        register(columns, displays, BusinessSemanticQuery.Field.START_DATE, "startDate", "开始日期");
        register(columns, displays, BusinessSemanticQuery.Field.END_DATE, "endDate", "结束日期");
        register(columns, displays, BusinessSemanticQuery.Field.MANAGEMENT_FEE_RATE,
                "managementFeeRate", "管理费率");
        register(columns, displays, BusinessSemanticQuery.Field.PRODUCT_TIER, "productTier", "产品分层");
        register(columns, displays, BusinessSemanticQuery.Field.FEE_ADJUSTMENT_PLAN,
                "feeAdjustmentPlan", "费率调整计划");
        register(columns, displays, BusinessSemanticQuery.Field.TRADE_SERIAL_NUMBER,
                "tradeSerialNumber", "交易流水号");
        register(columns, displays, BusinessSemanticQuery.Field.TRADER_NAME, "traderName", "交易员");
        register(columns, displays, BusinessSemanticQuery.Field.TRADE_DATE, "tradeDate", "交易日期");
        register(columns, displays, BusinessSemanticQuery.Field.PRODUCT_MATURITY_DATE,
                "productMaturityDate", "产品到期日期");
        register(columns, displays, BusinessSemanticQuery.Field.PERIODIC_OPEN_BASE_DATE,
                "periodicOpenBaseDate", "定开基准日期");
        register(columns, displays, BusinessSemanticQuery.Field.SNAPSHOT_DATE,
                "snapshotDate", "数据日期");
        COLUMN_NAMES = Collections.unmodifiableMap(columns);
        DISPLAY_NAMES = Collections.unmodifiableMap(displays);
    }

    /** 问答流程配置。 */
    private final QaProperties qaProperties;
    /** 业务查询通道配置。 */
    private final BusinessQueryProperties businessProperties;
    /** 一期业务语义解析器。 */
    private final PhaseOneBusinessSemanticParser semanticParser;
    /** 一期确定性查询规划器。 */
    private final PhaseOneBusinessQueryPlanner queryPlanner;
    /** 一期受控 SQL 编译器。 */
    private final PhaseOneBusinessSqlCompiler sqlCompiler;
    /** 一期业务语义视图映射器。 */
    private final BusinessQueryMapper queryMapper;
    /** 用于确定当前业务日的系统时钟。 */
    private final Clock clock;

    /**
     * 创建一期业务语义查询适配器。
     *
     * @param qaProperties 问答流程配置。
     * @param businessProperties 业务查询通道配置。
     * @param semanticParser 一期业务语义解析器。
     * @param queryPlanner 一期确定性查询规划器。
     * @param sqlCompiler 一期受控 SQL 编译器。
     * @param queryMapper 一期业务语义视图映射器。
     * @param clock 用于确定当前业务日的系统时钟。
     */
    public BusinessDataAdapter(
            final QaProperties qaProperties,
            final BusinessQueryProperties businessProperties,
            final PhaseOneBusinessSemanticParser semanticParser,
            final PhaseOneBusinessQueryPlanner queryPlanner,
            final PhaseOneBusinessSqlCompiler sqlCompiler,
            final BusinessQueryMapper queryMapper,
            final Clock clock) {
        this.qaProperties = Objects.requireNonNull(qaProperties, "qaProperties must not be null");
        this.businessProperties = Objects.requireNonNull(
                businessProperties, "businessProperties must not be null");
        this.semanticParser = Objects.requireNonNull(semanticParser, "semanticParser must not be null");
        this.queryPlanner = Objects.requireNonNull(queryPlanner, "queryPlanner must not be null");
        this.sqlCompiler = Objects.requireNonNull(sqlCompiler, "sqlCompiler must not be null");
        this.queryMapper = Objects.requireNonNull(queryMapper, "queryMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 通过一期受控语义模型查询 GoldenDB 业务事实。
     *
     * @param ownerId 用户所有者 ID。
     * @param question 用户问题。
     * @param intent 已完成消歧的查询意图。
     * @param limit 数量上限。
     * @return 可追溯业务事实列表。
     */
    @Override
    public List<BusinessFact> query(
            final String ownerId,
            final String question,
            final QueryIntent intent,
            final int limit) {
        if (!businessProperties.enabled()) {
            if (qaProperties.demoMode()) {
                return Collections.emptyList();
            }
            throw new DependencyUnavailableException(
                    "BUSINESS_QUERY_UNCONFIGURED", "approved business query catalog is not configured");
        }
        final BusinessSemanticQuery semanticQuery = semanticParser.parse(question, intent, limit);
        validatePhysicalMappings(semanticQuery);
        final BusinessQueryPlan plan = queryPlanner.plan(
                semanticQuery,
                businessProperties.semanticModelVersion(),
                businessProperties.maximumRows());
        final CompiledBusinessQuery compiled = sqlCompiler.compile(plan);
        try {
            return facts(compiled, execute(ownerId, compiled));
        } catch (final DataAccessException exception) {
            throw new DependencyUnavailableException(
                    "BUSINESS_QUERY_FAILED", "approved business query execution failed", exception);
        }
    }

    /**
     * 校验语义字段均有当前物理视图的明确映射。
     *
     * @param query 已完成语义解析的受控查询。
     */
    private void validatePhysicalMappings(final BusinessSemanticQuery query) {
        if (!PHYSICALLY_MAPPED_FIELDS.containsAll(query.fields())) {
            // 数据字典尚未确认的列不得靠空值或模型补齐，否则会形成无证据事实。
            throw new DependencyUnavailableException(
                    "BUSINESS_FIELD_MAPPING_UNCONFIGURED",
                    "requested business fields are not mapped by the approved physical view");
        }
    }

    /**
     * 执行编译后批准的 MyBatis 查询语句。
     *
     * @param ownerId 用户所有者 ID。
     * @param query 编译后的受控业务查询。
     * @return 物理语义视图记录。
     */
    private List<Map<String, Object>> execute(
            final String ownerId,
            final CompiledBusinessQuery query) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        if (query.statement() == CompiledBusinessQuery.Statement.SELECT_TRADE_FACTS) {
            return queryMapper.selectTradeFacts(
                    ownerId, query.entityReference().orElseThrow(IllegalStateException::new), query.limit());
        }
        final String businessDate = query.businessDate()
                .orElseGet(() -> LocalDate.now(clock.withZone(BUSINESS_ZONE)))
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
        final String partitionDate = queryMapper.selectLatestPartitionDate(businessDate);
        if (partitionDate == null || partitionDate.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (query.statement() == CompiledBusinessQuery.Statement.SELECT_REFERENCE_DATE_PRODUCTS) {
            return queryMapper.selectReferenceDateProducts(businessDate, partitionDate, query.limit());
        }
        return queryMapper.selectProductFacts(
                query.entityReference().orElseThrow(IllegalStateException::new), partitionDate, query.limit());
    }

    /**
     * 将受控查询记录转换为带计划和语义版本的业务事实。
     *
     * @param query 编译后的受控业务查询。
     * @param rows 物理语义视图记录。
     * @return 可追溯业务事实列表。
     */
    private List<BusinessFact> facts(
            final CompiledBusinessQuery query,
            final List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        final List<BusinessFact> facts = new ArrayList<>();
        for (final Map<String, Object> row : rows) {
            final String content = factContent(query, row);
            if (!content.isEmpty()) {
                facts.add(new BusinessFact(sourceCode(query, row), content));
            }
        }
        return Collections.unmodifiableList(facts);
    }

    /**
     * 构造单条业务事实的结构化文本，且只输出查询计划批准的字段。
     *
     * @param query 编译后的受控业务查询。
     * @param row 物理语义视图记录。
     * @return 业务事实结构化文本。
     */
    private String factContent(
            final CompiledBusinessQuery query,
            final Map<String, Object> row) {
        final StringBuilder content = new StringBuilder();
        for (final BusinessSemanticQuery.Field field : query.fields()) {
            final Object value = value(row, COLUMN_NAMES.get(field));
            if (value != null) {
                if (content.length() > 0) {
                    content.append(';');
                }
                content.append(DISPLAY_NAMES.get(field)).append('=').append(value);
            } else if (isManagerField(field)) {
                if (content.length() > 0) {
                    content.append(';');
                }
                content.append(DISPLAY_NAMES.get(field)).append("=暂未维护");
            }
        }
        appendMatchReasons(query, row, content);
        if (content.length() > 0) {
            content.append(";语义模型版本=").append(query.semanticModelVersion())
                    .append(";查询计划=").append(query.planId());
        }
        return content.toString();
    }

    /**
     * 判断字段是否为必须明确展示口径的经理字段。
     *
     * @param field 标准业务字段。
     * @return 需要在空值时展示“暂未维护”时返回 true。
     */
    private boolean isManagerField(final BusinessSemanticQuery.Field field) {
        return field == BusinessSemanticQuery.Field.REGULATORY_INVESTMENT_MANAGER
                || field == BusinessSemanticQuery.Field.DEPARTMENT_INVESTMENT_MANAGER
                || field == BusinessSemanticQuery.Field.PRODUCT_MANAGER;
    }

    /**
     * 为日期列表事实补充一个或两个确定性命中原因。
     *
     * @param query 编译后的受控业务查询。
     * @param row 物理查询结果行。
     * @param content 待追加的事实文本。
     */
    private void appendMatchReasons(
            final CompiledBusinessQuery query,
            final Map<String, Object> row,
            final StringBuilder content) {
        if (query.statement() != CompiledBusinessQuery.Statement.SELECT_REFERENCE_DATE_PRODUCTS) {
            return;
        }
        final List<String> reasons = new ArrayList<>();
        if (matched(value(row, "periodicOpenBaseDateMatched"))) {
            reasons.add("定开基准日");
        }
        if (matched(value(row, "productMaturityDateMatched"))) {
            reasons.add("产品到期日");
        }
        if (!reasons.isEmpty()) {
            content.append(";命中原因=").append(String.join("、", reasons));
        }
    }

    /**
     * 兼容不同 JDBC 驱动的布尔或数值命中标志。
     *
     * @param value 数据库返回的命中标志。
     * @return 标志表示命中时返回 true。
     */
    private boolean matched(final Object value) {
        return Boolean.TRUE.equals(value) || value instanceof Number
                && ((Number) value).intValue() == 1 || "1".equals(String.valueOf(value));
    }

    /**
     * 构造可追溯且不暴露物理表名的事实来源编码。
     *
     * @param query 编译后的受控业务查询。
     * @param row 物理语义视图记录。
     * @return 事实来源编码。
     */
    private String sourceCode(
            final CompiledBusinessQuery query,
            final Map<String, Object> row) {
        final String column = query.statement() == CompiledBusinessQuery.Statement.SELECT_TRADE_FACTS
                ? "tradeSerialNumber" : "productCode";
        final Object reference = value(row, column);
        return "goldendb:" + query.planId() + ':'
                + (reference == null ? query.entityReference().orElse("date-list") : reference.toString());
    }

    /**
     * 兼容不同 JDBC 驱动对结果别名大小写的处理并读取字段值。
     *
     * @param row 物理语义视图记录。
     * @param column 预定义结果别名。
     * @return 对应字段值，不存在时返回 null。
     */
    private Object value(final Map<String, Object> row, final String column) {
        if (row.containsKey(column)) {
            return row.get(column);
        }
        for (final Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(column)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 注册标准字段对应的固定物理别名和中文名称。
     *
     * @param columns 标准字段与物理别名映射。
     * @param displays 标准字段与中文名称映射。
     * @param field 标准字段。
     * @param column 固定物理结果别名。
     * @param display 中文业务名称。
     */
    private static void register(
            final Map<BusinessSemanticQuery.Field, String> columns,
            final Map<BusinessSemanticQuery.Field, String> displays,
            final BusinessSemanticQuery.Field field,
            final String column,
            final String display) {
        columns.put(field, column);
        displays.put(field, display);
    }
}
