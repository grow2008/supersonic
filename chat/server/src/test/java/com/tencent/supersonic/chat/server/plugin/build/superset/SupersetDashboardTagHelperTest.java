package com.tencent.supersonic.chat.server.plugin.build.superset;

import com.tencent.supersonic.common.pojo.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class SupersetDashboardTagHelperTest {

    @Test
    public void testBuildManualTagsIncludesOwner() {
        User user = User.get(9L, "alice");
        List<String> tags = SupersetDashboardTagHelper.buildManualTags(user);
        Assertions.assertTrue(tags.contains(SupersetDashboardTagHelper.TAG_SUPERSONIC));
        Assertions.assertTrue(tags.contains(SupersetDashboardTagHelper.TAG_MANUAL));
        Assertions.assertTrue(tags.contains("supersonic-owner-9"));
    }

    @Test
    public void testIsManualDashboardFiltersSingleChart() {
        SupersetDashboardInfo info = new SupersetDashboardInfo();
        info.setTags(Arrays.asList(SupersetDashboardTagHelper.TAG_MANUAL,
                SupersetDashboardTagHelper.TAG_SINGLE_CHART));
        Assertions.assertFalse(SupersetDashboardTagHelper.isManualDashboard(info));
    }

    @Test
    public void testIsOwnerMatchesOwnerTag() {
        User user = User.get(11L, "bob");
        SupersetDashboardInfo info = new SupersetDashboardInfo();
        info.setTags(Arrays.asList("supersonic-owner-11", "supersonic-manual"));
        Assertions.assertTrue(SupersetDashboardTagHelper.isOwner(user, info));
    }
}
