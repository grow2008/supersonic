package com.tencent.supersonic.chat.server.plugin.build.superset;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.PluginTool;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.service.PluginService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class SupersetPluginResolver {

    private SupersetPluginResolver() {
    }

    /**
     * 解析执行上下文可用的 Superset 插件。
     *
     * Args: executeContext: 执行上下文。
     *
     * Returns: 匹配的 Superset 插件。
     */
    public static Optional<ChatPlugin> resolveSupersetPlugin(ExecuteContext executeContext) {
        if (executeContext == null) {
            return Optional.empty();
        }
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
        SemanticParseInfo parseInfo = executeContext.getParseInfo();
        Long dataSetId = parseInfo == null ? null : parseInfo.getDataSetId();
        return plugins.stream().filter(plugin -> "SUPERSET".equalsIgnoreCase(plugin.getType()))
                .filter(plugin -> matchDataSet(plugin, dataSetId)).findFirst();
    }

    /**
     * 解析 Superset 插件配置。
     *
     * Args: plugin: Superset 插件实例。
     *
     * Returns: Superset 插件配置。
     */
    public static SupersetPluginConfig resolveSupersetConfig(ChatPlugin plugin) {
        if (plugin == null || StringUtils.isBlank(plugin.getConfig())) {
            return null;
        }
        return JsonUtil.toObject(plugin.getConfig(), SupersetPluginConfig.class);
    }

    /**
     * 判断 Superset 插件是否已启用且具备有效配置。
     *
     * Args: executeContext: 执行上下文。
     *
     * Returns: true 表示 Superset 可用。
     */
    public static boolean isSupersetEnabled(ExecuteContext executeContext) {
        Optional<ChatPlugin> pluginOptional = resolveSupersetPlugin(executeContext);
        if (!pluginOptional.isPresent()) {
            return false;
        }
        SupersetPluginConfig config = resolveSupersetConfig(pluginOptional.get());
        if (config == null || !config.isEnabled()) {
            return false;
        }
        if (StringUtils.isBlank(config.getBaseUrl())) {
            return false;
        }
        return config.hasValidAuthConfig();
    }

    private static List<Long> resolvePluginIds(Agent agent) {
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

    private static boolean matchDataSet(ChatPlugin plugin, Long dataSetId) {
        if (plugin.isContainsAllDataSet()) {
            return true;
        }
        if (dataSetId == null) {
            return true;
        }
        return plugin.getDataSetList() != null && plugin.getDataSetList().contains(dataSetId);
    }
}
