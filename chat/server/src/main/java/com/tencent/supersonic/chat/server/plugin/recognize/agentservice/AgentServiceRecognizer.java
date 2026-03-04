package com.tencent.supersonic.chat.server.plugin.recognize.agentservice;

import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.PluginManager;
import com.tencent.supersonic.chat.server.plugin.PluginParseResult;
import com.tencent.supersonic.chat.server.plugin.PluginRecallResult;
import com.tencent.supersonic.chat.server.plugin.recognize.PluginRecognizer;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;

import java.util.HashMap;
import java.util.Map;

public class AgentServiceRecognizer extends PluginRecognizer {

    @Override
    public boolean checkPreCondition(ParseContext parseContext) {
        return PluginManager.hasAgentServicePlugin(parseContext);
    }

    @Override
    public PluginRecallResult recallPlugin(ParseContext parseContext) {
        return null;
    }

    @Override
    public void recognize(ParseContext parseContext) {
        if (!checkPreCondition(parseContext)) {
            return;
        }
        ChatPlugin plugin = PluginManager.getAgentServicePlugin(parseContext);
        if (plugin == null) {
            return;
        }
        ChatParseResp response = parseContext.getResponse();
        response.getSelectedParses().clear();

        SemanticParseInfo parseInfo = new SemanticParseInfo();
        parseInfo.setId(1);
        parseInfo.setQueryMode(plugin.getType());
        parseInfo.setScore(1.0);

        PluginParseResult pluginParseResult = new PluginParseResult();
        pluginParseResult.setPlugin(plugin);
        pluginParseResult.setQueryText(parseContext.getRequest().getQueryText());

        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.CONTEXT, pluginParseResult);
        properties.put("type", "plugin");
        properties.put("name", plugin.getName());
        properties.put("agentId", parseContext.getRequest().getAgentId());
        properties.put("chatId", parseContext.getRequest().getChatId());
        parseInfo.setProperties(properties);
        parseInfo.setTextInfo(String.format("将由插件工具**%s**来解答", plugin.getName()));

        response.getSelectedParses().add(parseInfo);
        response.setState(ParseResp.ParseState.COMPLETED);
    }
}
