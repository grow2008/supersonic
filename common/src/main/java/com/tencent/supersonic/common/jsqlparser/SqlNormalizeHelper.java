package com.tencent.supersonic.common.jsqlparser;

import com.tencent.supersonic.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQL normalization utilities for stable hashing and table detection.
 */
@Slf4j
public class SqlNormalizeHelper {

    private SqlNormalizeHelper() {}

    /**
     * Normalize SQL for stable hashing.
     *
     * Args: sql: Raw SQL string.
     *
     * Returns: Normalized SQL string (no trailing semicolon, normalized spaces).
     */
    public static String normalizeSql(String sql) {
        if (StringUtils.isBlank(sql)) {
            return "";
        }
        String trimmed = stripTrailingSemicolon(sql.trim());
        try {
            Statement statement = CCJSqlParserUtil.parse(trimmed);
            String parsed = statement == null ? null : statement.toString();
            if (StringUtils.isNotBlank(parsed)) {
                return StringUtils.normalizeSpace(stripTrailingSemicolon(parsed));
            }
        } catch (Exception ex) {
            log.debug("normalize sql parse failed, fallback to trim", ex);
        }
        return StringUtils.normalizeSpace(trimmed);
    }

    /**
     * Extract table names from SQL.
     *
     * Args: sql: SQL string.
     *
     * Returns: Ordered set of normalized table names.
     */
    public static Set<String> extractTableNames(String sql) {
        if (StringUtils.isBlank(sql)) {
            return Collections.emptySet();
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement == null) {
                return Collections.emptySet();
            }
            TablesNamesFinder finder = new TablesNamesFinder();
            List<String> tables = finder.getTableList(statement);
            if (tables == null || tables.isEmpty()) {
                return Collections.emptySet();
            }
            return tables.stream().filter(StringUtils::isNotBlank)
                    .map(SqlNormalizeHelper::normalizeTableName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (Exception ex) {
            log.debug("extract table names failed", ex);
            return Collections.emptySet();
        }
    }

    /**
     * Check whether SQL touches a single table.
     *
     * Args: sql: SQL string.
     *
     * Returns: true if exactly one table is detected.
     */
    public static boolean isSingleTable(String sql) {
        return extractTableNames(sql).size() == 1;
    }

    private static String normalizeTableName(String name) {
        return StringUtil.replaceBackticks(StringUtils.trimToEmpty(name));
    }

    private static String stripTrailingSemicolon(String sql) {
        if (StringUtils.isBlank(sql)) {
            return "";
        }
        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
