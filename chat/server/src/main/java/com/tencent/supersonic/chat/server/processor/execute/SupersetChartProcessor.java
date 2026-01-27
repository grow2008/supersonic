package com.tencent.supersonic.chat.server.processor.execute;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.PluginTool;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.build.ParamOption;
import com.tencent.supersonic.chat.server.plugin.build.WebBase;
import com.tencent.supersonic.chat.server.plugin.build.superset.SupersetApiClient;
import com.tencent.supersonic.chat.server.plugin.build.superset.SupersetChartInfo;
import com.tencent.supersonic.chat.server.plugin.build.superset.SupersetChartResp;
import com.tencent.supersonic.chat.server.plugin.build.superset.SupersetDashboardInfo;
import com.tencent.supersonic.chat.server.plugin.build.superset.SupersetPluginConfig;
import com.tencent.supersonic.chat.server.plugin.build.superset.SupersetVizTypeSelector;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.service.PluginService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class SupersetChartProcessor implements ExecuteResultProcessor {

    public static final String QUERY_MODE = "SUPERSET";

    @Override
    public boolean accept(ExecuteContext executeContext) {
        QueryResult queryResult = executeContext.getResponse();
        if (queryResult == null || executeContext.getParseInfo() == null) {
            return false;
        }
        if (!QueryState.SUCCESS.equals(queryResult.getQueryState())) {
            return false;
        }
        String queryMode = queryResult.getQueryMode();
        return !StringUtils.equalsAnyIgnoreCase(queryMode, "WEB_PAGE", "WEB_SERVICE", "PLAIN_TEXT");
    }

    @Override
    public void process(ExecuteContext executeContext) {
        QueryResult queryResult = executeContext.getResponse();
        Optional<ChatPlugin> pluginOptional = resolveSupersetPlugin(executeContext);
        if (!pluginOptional.isPresent()) {
            return;
        }
        ChatPlugin plugin = pluginOptional.get();
        SupersetPluginConfig config =
                JsonUtil.toObject(plugin.getConfig(), SupersetPluginConfig.class);
        if (config == null || !config.isEnabled()) {
            return;
        }
        SupersetChartResp response = new SupersetChartResp();
        response.setName(plugin.getName());
        response.setPluginId(plugin.getId());
        response.setPluginType(plugin.getType());

        String sql = resolveSql(queryResult, executeContext.getParseInfo());
        String vizType = resolveVizType(config, queryResult);
        response.setVizType(vizType);
        if (StringUtils.isBlank(sql) || StringUtils.isBlank(config.getBaseUrl())
                || StringUtils.isBlank(config.getAccessToken())) {
            response.setFallback(true);
            response.setFallbackReason("superset config invalid");
            queryResult.setQueryMode(QUERY_MODE);
            queryResult.setResponse(response);
            return;
        }
        try {
            SupersetApiClient client = new SupersetApiClient(config);
            String chartName = buildChartName(executeContext, plugin);
            SupersetChartInfo chartInfo =
                    client.createEmbeddedChart(sql, chartName, vizType, config.getFormData());
            response.setChartId(chartInfo.getChartId());
            response.setChartUuid(chartInfo.getChartUuid());
            response.setGuestToken(chartInfo.getGuestToken());
            response.setWebPage(
                    buildWebPage(config, chartInfo.getChartUuid(), chartInfo.getGuestToken()));
            List<SupersetDashboardInfo> dashboards = Collections.emptyList();
            try {
                dashboards = client.listDashboards();
            } catch (Exception ex) {
                log.warn("superset dashboard list load failed", ex);
            }
            response.setDashboards(dashboards);
            response.setFallback(false);
        } catch (Exception ex) {
            log.warn("superset chart build failed", ex);
            response.setFallback(true);
            response.setFallbackReason(ex.getMessage());
        }
        queryResult.setQueryMode(QUERY_MODE);
        queryResult.setResponse(response);
    }

    private Optional<ChatPlugin> resolveSupersetPlugin(ExecuteContext executeContext) {
        PluginService pluginService = ContextUtils.getBean(PluginService.class);
        List<ChatPlugin> plugins = pluginService.getPluginList();
        if (CollectionUtils.isEmpty(plugins)) {
            return Optional.empty();
        }
        Agent agent = executeContext.getAgent();
        List<Long> pluginIds = resolvePluginIds(agent);
        if (!CollectionUtils.isEmpty(pluginIds)) {
            plugins = plugins.stream().filter(plugin -> pluginIds.contains(plugin.getId()))
                    .collect(Collectors.toList());
        }
        Long dataSetId = executeContext.getParseInfo().getDataSetId();
        return plugins.stream().filter(plugin -> "SUPERSET".equalsIgnoreCase(plugin.getType()))
                .filter(plugin -> matchDataSet(plugin, dataSetId)).findFirst();
    }

    private List<Long> resolvePluginIds(Agent agent) {
        if (agent == null) {
            return Collections.emptyList();
        }
        List<String> tools = agent.getTools(AgentToolType.PLUGIN);
        if (CollectionUtils.isEmpty(tools)) {
            return Collections.emptyList();
        }
        List<Long> pluginIds = new ArrayList<>();
        for (String tool : tools) {
            PluginTool pluginTool = JSONObject.parseObject(tool, PluginTool.class);
            if (pluginTool != null && !CollectionUtils.isEmpty(pluginTool.getPlugins())) {
                pluginIds.addAll(pluginTool.getPlugins());
            }
        }
        return pluginIds;
    }

    private boolean matchDataSet(ChatPlugin plugin, Long dataSetId) {
        if (plugin.isContainsAllDataSet()) {
            return true;
        }
        if (dataSetId == null) {
            return true;
        }
        return plugin.getDataSetList() != null && plugin.getDataSetList().contains(dataSetId);
    }

    private String resolveSql(QueryResult queryResult, SemanticParseInfo parseInfo) {
        if (queryResult != null && StringUtils.isNotBlank(queryResult.getQuerySql())) {
            return queryResult.getQuerySql();
        }
        if (parseInfo != null && parseInfo.getSqlInfo() != null) {
            return parseInfo.getSqlInfo().getCorrectedS2SQL();
        }
        return null;
    }

    private String resolveVizType(SupersetPluginConfig config, QueryResult queryResult) {
        if (StringUtils.isNotBlank(config.getVizType())
                && !"AUTO".equalsIgnoreCase(config.getVizType())) {
            return config.getVizType();
        }
        return SupersetVizTypeSelector.select(queryResult);
    }

    private String buildChartName(ExecuteContext executeContext, ChatPlugin plugin) {
        Long queryId = executeContext.getRequest().getQueryId();
        String suffix =
                queryId == null ? String.valueOf(System.currentTimeMillis()) : queryId.toString();
        return "supersonic_" + plugin.getName() + "_" + suffix;
    }

    private WebBase buildWebPage(SupersetPluginConfig config, String chartUuid, String guestToken) {
        WebBase webBase = new WebBase();
        webBase.setUrl(buildEmbedUrl(config, chartUuid));
        List<ParamOption> paramOptions = new ArrayList<>();
        if (config.getHeight() != null) {
            ParamOption heightOption = new ParamOption();
            heightOption.setParamType(ParamOption.ParamType.FORWARD);
            heightOption.setKey("height");
            heightOption.setValue(config.getHeight());
            paramOptions.add(heightOption);
        }
        if (StringUtils.isNotBlank(guestToken)) {
            ParamOption tokenOption = new ParamOption();
            tokenOption.setParamType(ParamOption.ParamType.CUSTOM);
            tokenOption.setKey("guestToken");
            tokenOption.setValue(guestToken);
            paramOptions.add(tokenOption);
        }
        webBase.setParamOptions(paramOptions);
        return webBase;
    }

    private String buildEmbedUrl(SupersetPluginConfig config, String chartUuid) {
        String baseUrl = StringUtils.defaultString(config.getBaseUrl());
        String normalized =
                baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String embedPath =
                StringUtils.defaultIfBlank(config.getEmbedPath(), "/superset/embed/chart/");
        if (embedPath.contains("{uuid}")) {
            return normalized + embedPath.replace("{uuid}", chartUuid);
        }
        if (!embedPath.startsWith("/")) {
            embedPath = "/" + embedPath;
        }
        if (!embedPath.endsWith("/")) {
            embedPath = embedPath + "/";
        }
        return normalized + embedPath + chartUuid + "/";
    }
}
