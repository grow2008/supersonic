package com.tencent.supersonic.chat.server.plugin.build.agentservice;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.PluginParseResult;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class AgentServiceQueryTest {

    private static class TestAgentServiceQuery extends AgentServiceQuery {
        private final Object response;

        private TestAgentServiceQuery(Object response) {
            this.response = response;
        }

        @Override
        protected Object callAgentService(String url, Map<String, Object> payload) {
            Assertions.assertEquals("http://example.com", url);
            Assertions.assertEquals("agent-1", payload.get("agent_id"));
            Assertions.assertEquals("12", payload.get("conversation_id"));
            Assertions.assertEquals("你好", payload.get("message"));
            return response;
        }
    }

    @Test
    public void build_shouldReturnTextResultFromResponse() {
        ChatPlugin plugin = new ChatPlugin();
        plugin.setType(AgentServiceQuery.QUERY_MODE);
        plugin.setParseModeConfig("{\"name\":\"agent-1\",\"examples\":[]}");
        plugin.setConfig("{\"url\":\"http://example.com\"}");

        PluginParseResult pluginParseResult = new PluginParseResult();
        pluginParseResult.setPlugin(plugin);
        pluginParseResult.setQueryText("你好");

        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.CONTEXT, pluginParseResult);
        properties.put("agentId", 1);
        properties.put("chatId", 2);

        SemanticParseInfo parseInfo = new SemanticParseInfo();
        parseInfo.setProperties(properties);

        TestAgentServiceQuery query =
                new TestAgentServiceQuery("{\"status\":\"ok\",\"data\":\"你好\"}");
        query.setParseInfo(parseInfo);

        QueryResult result = query.build();
        Assertions.assertEquals(QueryState.SUCCESS, result.getQueryState());
        Assertions.assertEquals("你好", result.getTextResult());
    }
}
