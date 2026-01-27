package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

@Data
public class SupersetDashboardPushReq {

    private Long pluginId;

    private Long dashboardId;

    private Long chartId;
}
