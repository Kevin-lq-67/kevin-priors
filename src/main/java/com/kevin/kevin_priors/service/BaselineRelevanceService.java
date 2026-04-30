package com.kevin.kevin_priors.service;

import com.kevin.kevin_priors.dto.Case;
import com.kevin.kevin_priors.dto.Prediction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service

public class BaselineRelevanceService implements RelevanceService {
    @Override
    public List<Prediction> predictForCase(Case c) {
        List<Prediction> predictions = new ArrayList<>();

        if(c.getPriorStudies() == null || c.getPriorStudies().isEmpty()) {
            log.debug("Case {} has no priors", c.getCaseId());
        }

        for(var prior : c.getPriorStudies()) {
            predictions.add(new Prediction(
                    c.getCaseId(),
                    prior.getStudyId(),
                    true
            ));
        }

        return predictions;
    }
}
