package com.acme.intelligentqa.adapter.out.business;

import com.acme.intelligentqa.domain.model.ProductInfoDto;
import com.acme.intelligentqa.domain.port.in.ProductSnapshotSyncUseCase;
import com.joyintech.datahub.model.FileMetadata;
import com.joyintech.datahub.subscribe.DhSubscribeBatchDataListener;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 产品信息文件下发与批量订阅监听适配器，接收 DataHub 推送的产品快照并持久化入库。
 */
@Component
public class ProductInfoFileListenerAdapter
        implements DhSubscribeBatchDataListener<ProductInfoDto> {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(ProductInfoFileListenerAdapter.class);


    /**
     * 产品快照同步用例。
     */
    private final ProductSnapshotSyncUseCase productSnapshotSyncUseCase;

    /**
     * 构造产品信息监听适配器。
     *
     * @param productSnapshotSyncUseCase 产品快照同步用例。
     */
    public ProductInfoFileListenerAdapter(final ProductSnapshotSyncUseCase productSnapshotSyncUseCase) {
        this.productSnapshotSyncUseCase = productSnapshotSyncUseCase;
    }

    /**
     * 获取监听器处理的数据传输对象类型。
     *
     * @return 产品信息数据传输对象类型。
     */
    @Override
    public Class<ProductInfoDto> getDataClass() {
        return ProductInfoDto.class;
    }

    /**
     * 处理批量产品数据下发事件。
     *
     * @param list 产品信息列表。
     * @param fileMetadata 文件元数据信息。
     */
    @Override
    public void onBatchData(final List<ProductInfoDto> list, final FileMetadata fileMetadata) {
        final String fallbackDate = fileMetadata != null ? fileMetadata.getDataDate() : null;
        log.info("监听到 DataHub 产品信息批次，批次大小：{}，元数据日期：{}",
                list != null ? list.size() : 0, fallbackDate);
        productSnapshotSyncUseCase.syncProductBatch(list, fallbackDate);
    }


    /**
     * 获取数据编码。
     *
     * @return 数据编码字符串。
     */
    @Override
    public String getDataCode() {
        return "ZH-0982-01-c463f8c51bb7";
    }

    /**
     * 获取数据版本号。
     *
     * @return 数据版本号。
     */
    @Override
    public String getDataVersion() {
        return "V1.0";
    }
}
