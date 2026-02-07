package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.headless.api.pojo.request.SupersetDatasetQueryReq;
import com.tencent.supersonic.headless.server.persistence.dataobject.SupersetDatasetDO;
import com.tencent.supersonic.headless.server.service.impl.SupersetDatasetRegistryServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Map;

public class SupersetDatasetRegistryServiceImplTest {

    @Test
    public void querySupersetDatasetShouldRejectNonAdmin() {
        SupersetDatasetRegistryServiceImpl service = buildService();
        User user = User.get(1L, "user");
        Assertions.assertThrows(InvalidPermissionException.class,
                () -> service.querySupersetDataset(new SupersetDatasetQueryReq(), user));
    }

    @Test
    public void deleteSupersetDatasetShouldRejectNonAdmin() {
        SupersetDatasetRegistryServiceImpl service = buildService();
        User user = User.get(1L, "user");
        Assertions.assertThrows(InvalidPermissionException.class,
                () -> service.deleteSupersetDataset(1L, user));
    }

    @Test
    public void deleteSupersetDatasetBatchShouldRejectNonAdmin() {
        SupersetDatasetRegistryServiceImpl service = buildService();
        User user = User.get(1L, "user");
        Assertions.assertThrows(InvalidPermissionException.class,
                () -> service.deleteSupersetDatasetBatch(Arrays.asList(1L, 2L), user));
    }

    @Test
    public void buildQueryWrapperShouldIncludeFilters() {
        TestableSupersetDatasetRegistryServiceImpl service = buildTestableService();
        SupersetDatasetQueryReq req = new SupersetDatasetQueryReq();
        req.setDatasetName("demo");
        req.setDatasetType("PHYSICAL");
        req.setDatabaseId(10L);
        req.setDataSetId(20L);
        req.setSqlHash("hash-1");
        req.setSupersetDatasetId(30L);
        req.setCreatedBy("alice");
        req.setSynced(true);

        LambdaQueryWrapper<SupersetDatasetDO> wrapper = service.buildWrapper(req);
        Map<String, Object> params = wrapper.getParamNameValuePairs();

        Assertions.assertTrue(params.containsValue("demo"));
        Assertions.assertTrue(params.containsValue("PHYSICAL"));
        Assertions.assertTrue(params.containsValue(10L));
        Assertions.assertTrue(params.containsValue(20L));
        Assertions.assertTrue(params.containsValue("hash-1"));
        Assertions.assertTrue(params.containsValue(30L));
        Assertions.assertTrue(params.containsValue("alice"));
    }

    private SupersetDatasetRegistryServiceImpl buildService() {
        DataSetService dataSetService = Mockito.mock(DataSetService.class);
        ModelService modelService = Mockito.mock(ModelService.class);
        DatabaseService databaseService = Mockito.mock(DatabaseService.class);
        return new SupersetDatasetRegistryServiceImpl(dataSetService, modelService,
                databaseService);
    }

    private TestableSupersetDatasetRegistryServiceImpl buildTestableService() {
        DataSetService dataSetService = Mockito.mock(DataSetService.class);
        ModelService modelService = Mockito.mock(ModelService.class);
        DatabaseService databaseService = Mockito.mock(DatabaseService.class);
        return new TestableSupersetDatasetRegistryServiceImpl(dataSetService, modelService,
                databaseService);
    }

    private static class TestableSupersetDatasetRegistryServiceImpl
            extends SupersetDatasetRegistryServiceImpl {
        private TestableSupersetDatasetRegistryServiceImpl(DataSetService dataSetService,
                ModelService modelService, DatabaseService databaseService) {
            super(dataSetService, modelService, databaseService);
        }

        private LambdaQueryWrapper<SupersetDatasetDO> buildWrapper(SupersetDatasetQueryReq req) {
            return buildQueryWrapper(req);
        }
    }
}
