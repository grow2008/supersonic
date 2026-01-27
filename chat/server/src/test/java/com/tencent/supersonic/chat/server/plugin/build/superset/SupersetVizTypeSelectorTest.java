package com.tencent.supersonic.chat.server.plugin.build.superset;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.common.pojo.QueryColumn;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class SupersetVizTypeSelectorTest {

    @Test
    public void testSelectLineForDateAndMetric() {
        QueryResult result = new QueryResult();
        QueryColumn dateColumn = new QueryColumn();
        dateColumn.setShowType("DATE");
        dateColumn.setType("DATE");
        QueryColumn metricColumn = new QueryColumn();
        metricColumn.setShowType("NUMBER");
        metricColumn.setType("BIGINT");
        result.setQueryColumns(Arrays.asList(dateColumn, metricColumn));
        result.setQueryResults(Arrays.asList(new HashMap<>(), new HashMap<>()));
        Assertions.assertEquals("line", SupersetVizTypeSelector.select(result));
    }

    @Test
    public void testSelectTableWhenEmpty() {
        QueryResult result = new QueryResult();
        result.setQueryColumns(Collections.emptyList());
        Assertions.assertEquals("table", SupersetVizTypeSelector.select(result));
    }

    @Test
    public void testSelectTableWhenNull() {
        Assertions.assertEquals("table", SupersetVizTypeSelector.select(null));
    }
}
