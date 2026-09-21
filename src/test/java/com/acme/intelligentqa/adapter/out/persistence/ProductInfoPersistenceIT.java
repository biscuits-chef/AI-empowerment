package com.acme.intelligentqa.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.acme.intelligentqa.adapter.out.persistence.mybatis.ProductInfoMapper;
import com.acme.intelligentqa.adapter.out.persistence.mybatis.ProductInfoPersistenceRecord;
import com.acme.intelligentqa.domain.model.ProductInfoDto;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * 验证产品快照 MyBatis 持久化仓储的批量入库、幂等更新与数据库交互。
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:product-persistence;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "mybatis-plus.mapper-locations=classpath*:/mapper/**/*.xml",
        "mybatis-plus.configuration.map-underscore-to-camel-case=false",
        "app.qa.demo-mode=true",
        "app.qa.cancellation.scan-delay-millis=60000"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Sql(scripts = "/db/business-query-test-schema.sql", config = @SqlConfig(encoding = "UTF-8"))
class ProductInfoPersistenceIT {

    /**
     * 产品快照持久化仓储。
     */
    @Autowired
    private MybatisProductSnapshotRepository repository;

    /**
     * 产品快照 MyBatis 映射器。
     */
    @Autowired
    private ProductInfoMapper mapper;

    /**
     * 验证批量保存新产品快照并成功落库。
     */
    @Test
    void savesBatchProductSnapshotsSuccessfully() {
        final ProductInfoDto dto1 = new ProductInfoDto();
        dto1.setPRDC_CD("IT_P001");
        dto1.setPRDC_NM("集成测试产品1");
        dto1.setEXPR_DT("2028-06-30");
        dto1.setACCT_DT("2026-09-18");

        final ProductInfoDto dto2 = new ProductInfoDto();
        dto2.setPRDC_CD("IT_P002");
        dto2.setPRDC_NM("集成测试产品2");
        dto2.setEXPR_DT("2029-12-31");
        dto2.setACCT_DT("2026-09-18");

        final int affected = repository.saveBatch(Arrays.asList(dto1, dto2), "2026-09-18");
        assertEquals(2, affected);

        final ProductInfoPersistenceRecord record1 = mapper.selectOne(
                new LambdaQueryWrapper<ProductInfoPersistenceRecord>()
                        .eq(ProductInfoPersistenceRecord::getPrdcCd, "IT_P001")
                        .eq(ProductInfoPersistenceRecord::getAcctDt, "2026-09-18"));
        assertNotNull(record1);
        assertEquals("集成测试产品1", record1.getPrdcNm());
        assertEquals("2028-06-30", record1.getExprDt());
    }

    /**
     * 验证同分区同产品重复保存时通过 ON DUPLICATE KEY UPDATE 覆盖更新已有数据。
     */
    @Test
    void updatesExistingProductOnDuplicateKey() {
        final ProductInfoDto initial = new ProductInfoDto();
        initial.setPRDC_CD("IT_DUP_01");
        initial.setPRDC_NM("初始产品名称");
        initial.setPRDC_MNGR_NM("初始经理");
        initial.setACCT_DT("2026-09-18");

        repository.saveBatch(Collections.singletonList(initial), "2026-09-18");

        final ProductInfoDto updated = new ProductInfoDto();
        updated.setPRDC_CD("IT_DUP_01");
        updated.setPRDC_NM("更新后产品名称");
        updated.setPRDC_MNGR_NM("更新后经理");
        updated.setACCT_DT("2026-09-18");

        repository.saveBatch(Collections.singletonList(updated), "2026-09-18");

        final List<ProductInfoPersistenceRecord> records = mapper.selectList(
                new LambdaQueryWrapper<ProductInfoPersistenceRecord>()
                        .eq(ProductInfoPersistenceRecord::getPrdcCd, "IT_DUP_01")
                        .eq(ProductInfoPersistenceRecord::getAcctDt, "2026-09-18"));
        assertEquals(1, records.size());
        assertEquals("更新后产品名称", records.get(0).getPrdcNm());
        assertEquals("更新后经理", records.get(0).getPrdcMngrNm());
    }
}
