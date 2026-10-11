package com.abhinay.frauddetection.dto;

import java.util.List;

public class VerificationResponse {

    private int mlPrediction;
    private String mlResult;
    private int ruleScore;
    private String finalResult;
    private List<String> reasons;

    public VerificationResponse() {
    }

    public int getMlPrediction() {
        return mlPrediction;
    }

    public void setMlPrediction(int mlPrediction) {
        this.mlPrediction = mlPrediction;
    }

    public String getMlResult() {
        return mlResult;
    }

    public void setMlResult(String mlResult) {
        this.mlResult = mlResult;
    }

    public int getRuleScore() {
        return ruleScore;
    }

    public void setRuleScore(int ruleScore) {
        this.ruleScore = ruleScore;
    }

    public String getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(String finalResult) {
        this.finalResult = finalResult;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}