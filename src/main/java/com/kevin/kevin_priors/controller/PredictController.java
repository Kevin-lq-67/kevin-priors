package com.kevin.kevin_priors.controller;

import com.kevin.kevin_priors.dto.Case;
import com.kevin.kevin_priors.dto.PredictRequest;
import com.kevin.kevin_priors.dto.PredictResponse;
import com.kevin.kevin_priors.dto.Prediction;
import com.kevin.kevin_priors.service.RelevanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PredictController {
    private final RelevanceService relevanceService;

    // run case-level Claude calls in parallel, otherwise big batches time out
    private static final int PARALLELISM = 16;

    @PostMapping("/predict")
    public ResponseEntity<PredictResponse> predict(@RequestBody PredictRequest request) {
        if(request == null || request.getCases() == null) {
            log.warn("Received empty or malformed request body");
            return ResponseEntity.badRequest().build();
        }

        log.info("Received predict request: {} cases", request.getCases().size());

        List<Prediction> all = new ArrayList<>();
        /*for(Case c : request.getCases()) {
            all.addAll(relevanceService.predictForCase(c));
        }
         */
        ExecutorService pool = Executors.newFixedThreadPool(PARALLELISM);
        try {
            List<Future<List<Prediction>>> futures = new ArrayList<>();
            for(Case c : request.getCases()) {
                futures.add(pool.submit(() -> relevanceService.predictForCase(c)));
            }
            for(Future<List<Prediction>> f : futures) {
                try {
                    all.addAll(f.get());
                } catch(Exception e) {
                    log.warn("Case task failed: {}", e.getMessage());
                }
            }
        } finally {
            pool.shutdown();
        }

        log.info("Returning {} predictions", all.size());
        return ResponseEntity.ok(new PredictResponse(all));
    }

    @GetMapping("/")
    public String health() {
        return "OK";
    }
}
