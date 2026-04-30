package com.kevin.kevin_priors.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Prediction {
    private String caseId;
    private String studyId;
    private Boolean predictedIsRelevant;
}
