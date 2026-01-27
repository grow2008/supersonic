package com.tencent.supersonic.chat.server.rest;

import com.tencent.supersonic.chat.api.pojo.request.SupersetDashboardListReq;
import com.tencent.supersonic.chat.api.pojo.request.SupersetDashboardPushReq;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.build.superset.SupersetApiClient;
import com.tencent.supersonic.chat.server.plugin.build.superset.SupersetDashboardInfo;
import com.tencent.supersonic.chat.server.plugin.build.superset.SupersetPluginConfig;
import com.tencent.supersonic.chat.server.service.PluginService;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping({"/api/chat/superset", "/openapi/chat/superset"})
public class SupersetController {

    @Autowired
    private PluginService pluginService;

    @PostMapping("dashboards")
    public List<SupersetDashboardInfo> dashboards(@RequestBody SupersetDashboardListReq req) {
        ChatPlugin plugin = resolveSupersetPlugin(req == null ? null : req.getPluginId());
        SupersetApiClient client = new SupersetApiClient(buildConfig(plugin));
        return client.listDashboards();
    }

    @PostMapping("dashboard/push")
    public boolean pushToDashboard(@RequestBody SupersetDashboardPushReq req) {
        if (req == null || req.getDashboardId() == null || req.getChartId() == null) {
            throw new InvalidArgumentException("dashboardId/chartId required");
        }
        ChatPlugin plugin = resolveSupersetPlugin(req.getPluginId());
        SupersetApiClient client = new SupersetApiClient(buildConfig(plugin));
        client.addChartToDashboard(req.getDashboardId(), req.getChartId());
        return true;
    }

    private ChatPlugin resolveSupersetPlugin(Long pluginId) {
        List<ChatPlugin> plugins = pluginService.getPluginList();
        if (plugins == null || plugins.isEmpty()) {
            throw new InvalidArgumentException("superset plugin missing");
        }
        Optional<ChatPlugin> candidate = plugins.stream()
                .filter(plugin -> "SUPERSET".equalsIgnoreCase(plugin.getType()))
                .filter(plugin -> pluginId == null || pluginId.equals(plugin.getId())).findFirst();
        if (!candidate.isPresent()) {
            throw new InvalidArgumentException("superset plugin not found");
        }
        return candidate.get();
    }

    private SupersetPluginConfig buildConfig(ChatPlugin plugin) {
        SupersetPluginConfig config =
                JsonUtil.toObject(plugin.getConfig(), SupersetPluginConfig.class);
        if (config == null || !config.isEnabled() || StringUtils.isBlank(config.getBaseUrl())
                || StringUtils.isBlank(config.getAccessToken())) {
            throw new InvalidArgumentException("superset config invalid");
        }
        return config;
    }
}
