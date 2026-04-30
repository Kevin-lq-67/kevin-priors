package com.kevin.kevin_priors.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
public class Case {
    private String caseId;
    private String patientId;
    private String patientName;
    private Study currentStudy;
    private List<Study> priorStudies;
}
