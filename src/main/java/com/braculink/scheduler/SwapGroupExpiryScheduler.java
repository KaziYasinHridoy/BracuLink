package com.braculink.scheduler;

import com.braculink.service.SwapGroupProposalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Releases swap groups nobody finished confirming.
 *
 * <p>The loop lives here rather than inside the service on purpose: calling a {@code @Transactional}
 * method on {@code this} would bypass Spring's proxy and silently run without a transaction. Going
 * through the injected service means every release is its own transaction, so one failing group
 * cannot strand the others.
 */
@Component
public class SwapGroupExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SwapGroupExpiryScheduler.class);

    private final SwapGroupProposalService swapGroupProposalService;

    public SwapGroupExpiryScheduler(SwapGroupProposalService swapGroupProposalService) {
        this.swapGroupProposalService = swapGroupProposalService;
    }

    @Scheduled(fixedRate = 3600000)
    public void releaseExpiredProposals() {
        List<Long> expired = swapGroupProposalService.findExpiredProposalIds();
        if (expired.isEmpty()) {
            return;
        }
        int released = 0;
        for (Long groupId : expired) {
            try {
                swapGroupProposalService.releaseExpiredGroup(groupId);
                released++;
            } catch (Exception e) {
                log.warn("Failed to release expired swap group {}: {}", groupId, e.getMessage());
            }
        }
        log.info("Released {} of {} expired swap groups", released, expired.size());
    }
}
