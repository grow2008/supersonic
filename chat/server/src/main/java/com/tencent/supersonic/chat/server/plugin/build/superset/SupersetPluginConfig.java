package com.tencent.supersonic.chat.server.plugin.build.superset;

import lombok.Data;

import java.util.Map;

@Data
public class SupersetPluginConfig {

    private boolean enabled = true;

    private String baseUrl;

    private String accessToken;

    private Integer timeoutSeconds = 30;

    private Long datasetId;

    private Long databaseId;

    private String schema;

    private String datasourceType = "table";

    private String vizType;

    private Map<String, Object> formData;

    private String embedPath;

    private Integer height;
}
