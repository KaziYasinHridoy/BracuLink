package com.braculink.service;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the one thing about the propose flow that fails silently.
 *
 * <p>A missing transaction manager, or a service Spring declined to proxy, does not break startup —
 * {@code @Transactional} simply stops doing anything and the propose flow quietly loses its
 * all-or-nothing guarantee. These assertions make that visible.
 *
 * <p>Needs a running MySQL, like every {@code @SpringBootTest} in this project.
 */
@SpringBootTest
@ActiveProfiles("test")
class SwapGroupTransactionWiringTest {

    /** Stops the course sync scheduler making a real network call during tests. */
    @MockitoBean
    private CourseSyncService courseSyncService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SwapGroupProposalService swapGroupProposalService;

    @Test
    void transactionManagerIsWired() {
        assertNotNull(transactionManager);
    }

    @Test
    void proposalServiceIsProxied() {
        assertTrue(AopUtils.isAopProxy(swapGroupProposalService),
                "SwapGroupProposalService must be proxied, or @Transactional does nothing");
    }

    @Test
    void theWriteMethodsCarryATransactionAttribute() throws NoSuchMethodException {
        AnnotationTransactionAttributeSource attributeSource = new AnnotationTransactionAttributeSource();

        for (Method method : List.of(
                SwapGroupProposalService.class.getMethod("propose", Long.class, List.class),
                SwapGroupProposalService.class.getMethod("confirm", Long.class, Long.class),
                SwapGroupProposalService.class.getMethod("decline", Long.class, Long.class),
                SwapGroupProposalService.class.getMethod("releaseExpiredGroup", Long.class))) {

            assertNotNull(attributeSource.getTransactionAttribute(method, SwapGroupProposalService.class),
                    method.getName() + "() must run in a transaction");
        }
    }
}
