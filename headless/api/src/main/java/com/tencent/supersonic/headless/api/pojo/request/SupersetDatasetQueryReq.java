package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.PageBaseReq;
import lombok.Data;

@Data
public class SupersetDatasetQueryReq extends PageBaseReq {

    private String datasetName;

    private String datasetType;

    private Long databaseId;

    private Long dataSetId;

    private String sqlHash;

    private Long supersetDatasetId;

    private String createdBy;

    private Boolean synced;
}
