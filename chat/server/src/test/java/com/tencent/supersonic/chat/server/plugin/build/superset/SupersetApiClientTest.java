package com.tencent.supersonic.chat.server.plugin.build.superset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupersetApiClientTest {

    @Test
    public void testExtractDashboards() {
        SupersetPluginConfig config = new SupersetPluginConfig();
        SupersetApiClient client = new SupersetApiClient(config);
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("id", 12L);
        dashboard.put("dashboard_title", "Sales Overview");
        result.add(dashboard);
        response.put("result", result);

        List<SupersetDashboardInfo> dashboards = client.extractDashboards(response);
        Assertions.assertEquals(1, dashboards.size());
        Assertions.assertEquals(12L, dashboards.get(0).getId());
        Assertions.assertEquals("Sales Overview", dashboards.get(0).getTitle());
    }
}
