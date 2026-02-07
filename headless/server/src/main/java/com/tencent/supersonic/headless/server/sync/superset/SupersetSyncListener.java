package com.tencent.supersonic.headless.server.sync.superset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SupersetSyncListener {

    private final SupersetSyncService supersetSyncService;

    public SupersetSyncListener(SupersetSyncService supersetSyncService) {
        this.supersetSyncService = supersetSyncService;
    }

    @Async("eventExecutor")
    @EventListener
    public void onSupersetSyncEvent(SupersetSyncEvent event) {
        if (event == null) {
            return;
        }
        if (event.getSyncType() == SupersetSyncType.DATABASE) {
            supersetSyncService.triggerDatabaseSync(event.getResourceIds(),
                    SupersetSyncTrigger.EVENT);
        }
    }
}
