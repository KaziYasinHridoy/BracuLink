package com.braculink.service;

import com.braculink.common.ApiException;
import com.braculink.dao.SwapRequestDao;
import com.braculink.dto.SwapSuggestionDto;
import com.braculink.model.SwapRequest;
import com.braculink.swap.engine.CycleMatchingService;
import com.braculink.swap.engine.SwapGraph;
import com.braculink.swap.engine.SwapRequestView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SwapSuggestionService {

    private static final String PENDING = "PENDING";

    private static final String NOT_FOUND_MESSAGE = "Swap request not found";
    private static final String NOT_PENDING_MESSAGE = "Suggestions are only available for pending swap requests";

    private final SwapRequestDao swapRequestDao;
    private final CycleMatchingService cycleMatchingService;

    public SwapSuggestionService(SwapRequestDao swapRequestDao, CycleMatchingService cycleMatchingService) {
        this.swapRequestDao = swapRequestDao;
        this.cycleMatchingService = cycleMatchingService;
    }

    public List<SwapSuggestionDto> getSuggestions(Long requestId, Long userId) {
        SwapRequest request = swapRequestDao.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE));

        // Someone else's request reports as missing rather than forbidden: suggestions expose other
        // students' names and IDs, so a distinguishable 403 would let a caller enumerate which
        // request ids exist. Matches how cancel() already behaves.
        if (!request.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE);
        }
        if (!PENDING.equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, NOT_PENDING_MESSAGE);
        }

        List<SwapRequestView> views = swapRequestDao.findActiveViewsByCourse(request.getCourseCode());

        // Take the caller's own row from the loaded set rather than rebuilding it from the model.
        // It re-checks the status (the request can flip to RESERVED between the two queries) and it
        // supplies the caller's name and section names for members[0].
        SwapRequestView me = views.stream()
                .filter(view -> view.getRequestId() == requestId)
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, NOT_PENDING_MESSAGE));

        if (!PENDING.equals(me.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, NOT_PENDING_MESSAGE);
        }

        SwapGraph graph = SwapGraph.build(request.getCourseCode(), views);
        return cycleMatchingService.findCandidates(graph, me, CycleMatchingService.DEFAULT_MAX_GROUP_SIZE);
    }
}
