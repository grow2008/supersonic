package com.tencent.supersonic.chat.server.plugin.build.superset;

import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class SupersetApiClient {

    private static final String DATASET_API = "/api/v1/dataset/";
    private static final String CHART_API = "/api/v1/chart/";
    private static final String GUEST_TOKEN_API = "/api/v1/security/guest_token/";
    private static final String DASHBOARD_API = "/api/v1/dashboard/";

    private final SupersetPluginConfig config;
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SupersetApiClient(SupersetPluginConfig config) {
        this.config = config;
        int timeoutMs =
                Math.max(1, config.getTimeoutSeconds() == null ? 30 : config.getTimeoutSeconds())
                        * 1000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
        this.baseUrl = normalizeBaseUrl(config.getBaseUrl());
    }

    public SupersetChartInfo createEmbeddedChart(String sql, String chartName, String vizType,
            Map<String, Object> formData) {
        Long datasetId = config.getDatasetId();
        if (datasetId == null) {
            datasetId = createDataset(sql);
        }
        Long chartId = createChart(datasetId, vizType, formData, chartName);
        String chartUuid = fetchChartUuid(chartId);
        String guestToken = createGuestToken(chartId);

        SupersetChartInfo chartInfo = new SupersetChartInfo();
        chartInfo.setDatasetId(datasetId);
        chartInfo.setChartId(chartId);
        chartInfo.setChartUuid(chartUuid);
        chartInfo.setGuestToken(guestToken);
        return chartInfo;
    }

    public List<SupersetDashboardInfo> listDashboards() {
        Map<String, Object> response = get(DASHBOARD_API + "?q=(page:0,page_size:200)");
        return extractDashboards(response);
    }

    public void addChartToDashboard(Long dashboardId, Long chartId) {
        if (dashboardId == null || chartId == null) {
            throw new IllegalStateException("superset dashboardId or chartId missing");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("chart_id", chartId);
        post(DASHBOARD_API + dashboardId + "/charts/", payload);
    }

    private Long createDataset(String sql) {
        if (config.getDatabaseId() == null) {
            throw new IllegalStateException("superset databaseId is required");
        }
        String datasetName = "supersonic_dataset_" + System.currentTimeMillis();
        Map<String, Object> payload = new HashMap<>();
        payload.put("database", config.getDatabaseId());
        payload.put("schema", config.getSchema());
        payload.put("table_name", StringUtils.defaultIfBlank(datasetName, "supersonic_dataset"));
        payload.put("sql", sql);
        payload.put("template_params", "{}");
        Map<String, Object> response = post(DATASET_API, payload);
        Long datasetId = parseLong(resolveValue(response, "id"));
        if (datasetId == null) {
            Map<String, Object> result = resolveMap(response, "result");
            datasetId = parseLong(resolveValue(result, "id"));
        }
        if (datasetId == null) {
            throw new IllegalStateException("superset dataset create failed");
        }
        return datasetId;
    }

    private Long createChart(Long datasetId, String vizType, Map<String, Object> formData,
            String chartName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("slice_name", chartName);
        payload.put("viz_type", vizType);
        payload.put("datasource_id", datasetId);
        payload.put("datasource_type", getDatasourceType());
        Map<String, Object> mergedFormData =
                formData == null ? new HashMap<>() : new HashMap<>(formData);
        mergedFormData.putIfAbsent("viz_type", vizType);
        mergedFormData.putIfAbsent("datasource", datasetId + "__" + getDatasourceType());
        payload.put("params", JsonUtil.toString(mergedFormData));
        Map<String, Object> response = post(CHART_API, payload);
        Long chartId = parseLong(resolveValue(response, "id"));
        if (chartId == null) {
            Map<String, Object> result = resolveMap(response, "result");
            chartId = parseLong(resolveValue(result, "id"));
        }
        if (chartId == null) {
            throw new IllegalStateException("superset chart create failed");
        }
        return chartId;
    }

    private String fetchChartUuid(Long chartId) {
        if (chartId == null) {
            throw new IllegalStateException("superset chartId missing");
        }
        Map<String, Object> response = get(CHART_API + chartId);
        String uuid = resolveString(response, "uuid");
        if (StringUtils.isBlank(uuid)) {
            Map<String, Object> result = resolveMap(response, "result");
            uuid = resolveString(result, "uuid");
        }
        if (StringUtils.isBlank(uuid)) {
            throw new IllegalStateException("superset chart uuid missing");
        }
        return uuid;
    }

    private String createGuestToken(Long chartId) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("type", "chart");
        resource.put("id", chartId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("resources", new Object[] {resource});
        payload.put("rls", new Object[] {});
        Map<String, Object> response = post(GUEST_TOKEN_API, payload);
        String token = resolveString(response, "token");
        if (StringUtils.isBlank(token)) {
            Map<String, Object> result = resolveMap(response, "result");
            token = resolveString(result, "token");
        }
        if (StringUtils.isBlank(token)) {
            throw new IllegalStateException("superset guest token missing");
        }
        return token;
    }

    private Map<String, Object> post(String path, Object body) {
        HttpHeaders headers = buildHeaders();
        HttpEntity<String> entity = new HttpEntity<>(JsonUtil.toString(body), headers);
        String url = baseUrl + path;
        ResponseEntity<Object> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
        return JsonUtil.objectToMap(response.getBody());
    }

    private Map<String, Object> get(String path) {
        HttpHeaders headers = buildHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = baseUrl + path;
        ResponseEntity<Object> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, Object.class);
        return JsonUtil.objectToMap(response.getBody());
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(config.getAccessToken())) {
            headers.set("Authorization", "Bearer " + config.getAccessToken());
        }
        return headers;
    }

    private String getDatasourceType() {
        return StringUtils.isBlank(config.getDatasourceType()) ? "table"
                : config.getDatasourceType();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private Object resolveValue(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    private Map<String, Object> resolveMap(Map<String, Object> map, String key) {
        Object value = resolveValue(map, key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    private List<Map<String, Object>> resolveList(Map<String, Object> map, String key) {
        Object value = resolveValue(map, key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return null;
    }

    private String resolveString(Map<String, Object> map, String key) {
        Object value = resolveValue(map, key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private Long parseLong(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            log.warn("superset id parse failed:{}", value);
            return null;
        }
    }

    List<SupersetDashboardInfo> extractDashboards(Map<String, Object> response) {
        if (response == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = resolveList(response, "result");
        if (result == null) {
            Map<String, Object> wrapper = resolveMap(response, "result");
            if (wrapper != null) {
                result = resolveList(wrapper, "result");
            }
        }
        if (result == null) {
            return Collections.emptyList();
        }
        List<SupersetDashboardInfo> dashboards = new ArrayList<>();
        for (Map<String, Object> item : result) {
            if (item == null) {
                continue;
            }
            SupersetDashboardInfo info = new SupersetDashboardInfo();
            info.setId(parseLong(resolveValue(item, "id")));
            String title = resolveString(item, "dashboard_title");
            if (StringUtils.isBlank(title)) {
                title = resolveString(item, "title");
            }
            info.setTitle(title);
            if (info.getId() != null || StringUtils.isNotBlank(info.getTitle())) {
                dashboards.add(info);
            }
        }
        return dashboards;
    }
}
