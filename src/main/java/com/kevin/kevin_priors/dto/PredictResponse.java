package com.kevin.kevin_priors.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PredictResponse {
    private List<Prediction> predictions;
}
