package com.kevin.kevin_priors.service;

import com.kevin.kevin_priors.dto.Case;
import com.kevin.kevin_priors.dto.Prediction;

import java.util.List;
public interface RelevanceService {
    List<Prediction> predictForCase(Case c);
}
