package com.tencent.supersonic.chat.server.plugin.build.superset;

import com.tencent.supersonic.chat.server.plugin.build.WebBase;
import lombok.Data;

import java.util.List;

@Data
public class SupersetChartResp {

    private Long pluginId;

    private String pluginType;

    private String name;

    private WebBase webPage;

    private boolean fallback;

    private String fallbackReason;

    private String vizType;

    private Long chartId;

    private String chartUuid;

    private String guestToken;

    private List<SupersetDashboardInfo> dashboards;
}
