package com.tencent.supersonic.chat.server.plugin.build.superset;

import lombok.Data;

@Data
public class SupersetChartInfo {

    private Long chartId;

    private String chartUuid;

    private String guestToken;

    private Long datasetId;
}
