package com.tencent.supersonic.headless.server.sync.superset;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.SupersetDatasetDO;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.service.SupersetDatasetRegistryService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupersetSyncServiceTest {

    @Test
    void syncDatasetsCreatesAndUpdatesRegistry() {
        SupersetSyncClient client = Mockito.mock(SupersetSyncClient.class);
        DatabaseService databaseService = Mockito.mock(DatabaseService.class);
        SupersetDatasetRegistryService registryService =
                Mockito.mock(SupersetDatasetRegistryService.class);
        SupersetSyncProperties properties = buildProperties();

        DatabaseResp databaseResp = buildDatabaseResp();
        when(databaseService.getDatabaseList(User.getDefaultUser()))
                .thenReturn(Collections.singletonList(databaseResp));
        when(databaseService.getDatabase(eq(1L), any(User.class))).thenReturn(databaseResp);

        SupersetDatabaseInfo supersetDatabase = new SupersetDatabaseInfo();
        supersetDatabase.setId(10L);
        supersetDatabase.setName("supersonic_db_1_demo");
        supersetDatabase.setSqlalchemyUri("postgresql+psycopg2://user:pass@localhost:5432/demo");
        supersetDatabase.setSchema("public");
        when(client.listDatabases()).thenReturn(Collections.singletonList(supersetDatabase));
        when(client.fetchDatabase(10L)).thenReturn(supersetDatabase);

        SupersetDatasetDO dataset = buildDatasetDO();
        when(registryService.listForSync(Collections.emptySet()))
                .thenReturn(Collections.singletonList(dataset));

        when(client.listDatasets()).thenReturn(Collections.emptyList());
        when(client.createDataset(any())).thenReturn(100L);
        SupersetDatasetInfo current = new SupersetDatasetInfo();
        current.setId(100L);
        current.setDatabaseId(10L);
        current.setTableName(dataset.getTableName());
        when(client.fetchDataset(100L)).thenReturn(current);

        SupersetSyncService service =
                new SupersetSyncService(client, properties, databaseService, registryService);
        SupersetSyncResult result =
                service.triggerDatasetSync(Collections.emptySet(), SupersetSyncTrigger.MANUAL);

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getStats().getCreated(), 1);
        verify(client, times(1)).createDataset(any(SupersetDatasetInfo.class));
        verify(client, times(1)).updateDataset(eq(100L), any(SupersetDatasetInfo.class));
        verify(registryService, times(1)).updateSyncInfo(eq(1L), eq(100L), any(Date.class));
    }

    @Test
    void buildDatasetInfoFallsBackToVirtualWhenTableMissing() {
        SupersetSyncClient client = Mockito.mock(SupersetSyncClient.class);
        DatabaseService databaseService = Mockito.mock(DatabaseService.class);
        SupersetDatasetRegistryService registryService =
                Mockito.mock(SupersetDatasetRegistryService.class);
        SupersetSyncProperties properties = buildProperties();

        DatabaseResp databaseResp = buildDatabaseResp();
        when(databaseService.getDatabaseList(User.getDefaultUser()))
                .thenReturn(Collections.singletonList(databaseResp));
        when(databaseService.getDatabase(eq(1L), any(User.class))).thenReturn(databaseResp);

        SupersetDatabaseInfo supersetDatabase = new SupersetDatabaseInfo();
        supersetDatabase.setId(10L);
        supersetDatabase.setName("supersonic_db_1_demo");
        supersetDatabase.setSqlalchemyUri("postgresql+psycopg2://user:pass@localhost:5432/demo");
        supersetDatabase.setSchema("public");
        when(client.listDatabases()).thenReturn(Collections.singletonList(supersetDatabase));
        when(client.fetchDatabase(10L)).thenReturn(supersetDatabase);

        SupersetDatasetDO dataset = buildDatasetDO();
        dataset.setDatasetType(SupersetDatasetType.PHYSICAL.name());
        dataset.setTableName(null);
        when(registryService.listForSync(Collections.emptySet()))
                .thenReturn(Collections.singletonList(dataset));

        when(client.listDatasets()).thenReturn(Collections.emptyList());
        when(client.createDataset(any())).thenReturn(101L);

        SupersetSyncService service =
                new SupersetSyncService(client, properties, databaseService, registryService);
        SupersetSyncResult result =
                service.triggerDatasetSync(Collections.emptySet(), SupersetSyncTrigger.MANUAL);

        Assert.assertTrue(result.isSuccess());
        ArgumentCaptor<SupersetDatasetInfo> captor =
                ArgumentCaptor.forClass(SupersetDatasetInfo.class);
        verify(client, times(1)).createDataset(captor.capture());
        SupersetDatasetInfo info = captor.getValue();
        Assert.assertNotNull(info);
        Assert.assertNotNull(info.getSql());
    }

    @Test
    void syncDatabasesFiltersUnsupportedJdbcParams() {
        SupersetSyncClient client = Mockito.mock(SupersetSyncClient.class);
        DatabaseService databaseService = Mockito.mock(DatabaseService.class);
        SupersetDatasetRegistryService registryService =
                Mockito.mock(SupersetDatasetRegistryService.class);
        SupersetSyncProperties properties = buildProperties();

        DatabaseResp databaseResp = DatabaseResp.builder().id(1L).name("demo").type("POSTGRESQL")
                .url("jdbc:postgresql://localhost:5432/demo?stringtype=unspecified&sslmode=disable")
                .username("user").password("pass").database("demo").schema("public").build();
        when(databaseService.getDatabaseList(User.getDefaultUser()))
                .thenReturn(Collections.singletonList(databaseResp));
        when(client.listDatabases()).thenReturn(Collections.emptyList());
        when(client.createDatabase(any())).thenReturn(10L);

        SupersetSyncService service =
                new SupersetSyncService(client, properties, databaseService, registryService);
        SupersetSyncResult result =
                service.triggerDatabaseSync(Collections.emptySet(), SupersetSyncTrigger.MANUAL);

        Assert.assertTrue(result.isSuccess());
        ArgumentCaptor<SupersetDatabaseInfo> captor =
                ArgumentCaptor.forClass(SupersetDatabaseInfo.class);
        verify(client, times(1)).createDatabase(captor.capture());
        SupersetDatabaseInfo databaseInfo = captor.getValue();
        Assert.assertNotNull(databaseInfo);
        Assert.assertFalse(databaseInfo.getSqlalchemyUri().contains("stringtype"));
        Assert.assertTrue(databaseInfo.getSqlalchemyUri().contains("sslmode=disable"));
    }

    private SupersetSyncProperties buildProperties() {
        SupersetSyncProperties properties = new SupersetSyncProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost:8088");
        return properties;
    }

    private DatabaseResp buildDatabaseResp() {
        return DatabaseResp.builder().id(1L).name("demo").type("POSTGRESQL")
                .url("jdbc:postgresql://localhost:5432/demo").username("user").password("pass")
                .database("demo").schema("public").build();
    }

    private SupersetDatasetDO buildDatasetDO() {
        SupersetDatasetDO dataset = new SupersetDatasetDO();
        dataset.setId(1L);
        dataset.setSqlHash("abc123");
        dataset.setSqlText("select * from t");
        dataset.setNormalizedSql("select * from t");
        dataset.setDatasetName("对话查询数据集·abc123");
        dataset.setDatasetDesc("对话查询数据集");
        dataset.setTags("[\"supersonic\",\"chat\"]");
        dataset.setDatasetType(SupersetDatasetType.VIRTUAL.name());
        dataset.setDataSetId(11L);
        dataset.setDatabaseId(1L);
        dataset.setSchemaName("public");
        dataset.setTableName("对话查询数据集·abc123");
        dataset.setMainDttmCol("ds");
        SupersetDatasetColumn column = new SupersetDatasetColumn();
        column.setColumnName("ds");
        column.setType("DATE");
        column.setIsDttm(true);
        dataset.setColumns(JsonUtil.toString(Collections.singletonList(column)));
        dataset.setMetrics(JsonUtil.toString(Collections.emptyList()));
        dataset.setCreatedAt(new Date());
        dataset.setUpdatedAt(new Date());
        return dataset;
    }

}
