package com.tencent.supersonic.chat.server.plugin.build.superset;

import com.tencent.supersonic.common.pojo.User;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SupersetDashboardTagHelper {

    public static final String TAG_SUPERSONIC = "supersonic";
    public static final String TAG_MANUAL = "supersonic-manual";
    public static final String TAG_SINGLE_CHART = "supersonic-single-chart";
    private static final String TAG_OWNER_PREFIX = "supersonic-owner-";

    private SupersetDashboardTagHelper() {}

    public static List<String> buildManualTags(User user) {
        List<String> tags = new ArrayList<>();
        tags.add(TAG_SUPERSONIC);
        tags.add(TAG_MANUAL);
        String ownerTag = buildOwnerTag(user);
        if (StringUtils.isNotBlank(ownerTag)) {
            tags.add(ownerTag);
        }
        return tags;
    }

    public static boolean isManualDashboard(SupersetDashboardInfo info) {
        if (info == null) {
            return false;
        }
        return hasTag(info, TAG_MANUAL) && !hasTag(info, TAG_SINGLE_CHART);
    }

    public static boolean isOwner(User user, SupersetDashboardInfo info) {
        if (user == null || info == null) {
            return false;
        }
        String ownerTag = buildOwnerTag(user);
        if (StringUtils.isBlank(ownerTag)) {
            return false;
        }
        return hasTag(info, ownerTag);
    }

    public static String buildOwnerTag(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        return TAG_OWNER_PREFIX + user.getId();
    }

    private static boolean hasTag(SupersetDashboardInfo info, String tag) {
        if (info == null || StringUtils.isBlank(tag)) {
            return false;
        }
        List<String> tags = info.getTags();
        if (tags == null || tags.isEmpty()) {
            return false;
        }
        return tags.stream().filter(StringUtils::isNotBlank).anyMatch(tag::equalsIgnoreCase);
    }

    public static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<>();
        for (String tag : tags) {
            if (StringUtils.isNotBlank(tag)) {
                normalized.add(tag.trim());
            }
        }
        return normalized;
    }
}
