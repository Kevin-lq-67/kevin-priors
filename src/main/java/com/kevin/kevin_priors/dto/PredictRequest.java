package com.kevin.kevin_priors.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
public class PredictRequest {
    private String challengeId;
    private Integer schemaVersion;
    private String generatedAt;
    private List<Case> cases;
}
