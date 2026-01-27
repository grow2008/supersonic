package com.tencent.supersonic.chat.server.plugin.build.superset;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.common.pojo.QueryColumn;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SupersetVizTypeSelector {

    private static final String DEFAULT_VIZ_TYPE = "table";
    private static final Set<String> NUMBER_TYPES =
            Stream.of("INT", "INTEGER", "BIGINT", "LONG", "FLOAT", "DOUBLE", "DECIMAL", "NUMBER")
                    .collect(Collectors.toSet());
    private static final Set<String> DATE_TYPES =
            Stream.of("DATE", "DATETIME", "TIMESTAMP").collect(Collectors.toSet());

    public static String select(QueryResult queryResult) {
        if (queryResult == null || queryResult.getQueryColumns() == null) {
            return DEFAULT_VIZ_TYPE;
        }
        List<QueryColumn> columns = queryResult.getQueryColumns();
        List<Map<String, Object>> results = queryResult.getQueryResults();
        if (columns.isEmpty()) {
            return DEFAULT_VIZ_TYPE;
        }

        long metricCount = columns.stream().filter(SupersetVizTypeSelector::isNumber).count();
        long dateCount = columns.stream().filter(SupersetVizTypeSelector::isDate).count();
        long categoryCount = columns.stream()
                .filter(column -> "CATEGORY".equalsIgnoreCase(column.getShowType())).count();

        if (metricCount == 1 && columns.size() == 1 && results != null && results.size() == 1) {
            return "big_number";
        }

        if (dateCount >= 1 && metricCount >= 1 && categoryCount <= 1) {
            return "line";
        }

        if (categoryCount >= 1 && metricCount == 1) {
            return "bar";
        }

        if (categoryCount == 1 && metricCount == 1 && results != null && results.size() <= 10) {
            return "pie";
        }

        return DEFAULT_VIZ_TYPE;
    }

    private static boolean isNumber(QueryColumn column) {
        if (column == null) {
            return false;
        }
        if ("NUMBER".equalsIgnoreCase(column.getShowType())) {
            return true;
        }
        String type = StringUtils.upperCase(column.getType());
        return StringUtils.isNotBlank(type) && NUMBER_TYPES.contains(type);
    }

    private static boolean isDate(QueryColumn column) {
        if (column == null) {
            return false;
        }
        if ("DATE".equalsIgnoreCase(column.getShowType())) {
            return true;
        }
        String type = StringUtils.upperCase(column.getType());
        return StringUtils.isNotBlank(type) && DATE_TYPES.contains(type);
    }
}
