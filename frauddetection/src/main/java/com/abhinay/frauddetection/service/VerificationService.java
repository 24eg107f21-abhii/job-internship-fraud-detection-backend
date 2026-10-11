
package com.abhinay.frauddetection.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.abhinay.frauddetection.dto.MLPredictionResponse;
import com.abhinay.frauddetection.dto.VerificationResponse;

@Service
public class VerificationService {

    private final MLService mlService;

    public VerificationService(MLService mlService) {
        this.mlService = mlService;
    }

    public VerificationResponse verifyJob(Map<String, Object> jobData) {

        MLPredictionResponse mlResponse = mlService.predictJob(jobData);

        int ruleScore = 0;
        List<String> reasons = new ArrayList<>();

        String title = getString(jobData, "title", "job_title", "jobTitle");
        String companyProfile = getString(jobData, "company_profile", "companyProfile");
        String company = getString(jobData, "company", "company_name", "companyName");
        String salary = getString(jobData, "salary_range", "salary", "salaryRange");
        String description = getString(jobData, "description", "job_description", "jobDescription");
        String requirements = getString(jobData, "requirements");
        String benefits = getString(jobData, "benefits");
        String location = getString(jobData, "location");

        String allText = normalize(
                title + " " + companyProfile + " " + company + " "
                + description + " " + requirements + " " + benefits
        );

        // Missing-information checks
        if (companyProfile.isBlank() && company.isBlank()) {
            ruleScore += 10;
            reasons.add("Company information is missing");
        }

        if (salary.isBlank()) {
            ruleScore += 5;
            reasons.add("Salary or stipend information is missing");
        }

        if (hasLogoField(jobData)
                && getInt(jobData, "has_company_logo", "hasCompanyLogo") == 0) {
            ruleScore += 5;
            reasons.add("Company logo is not available");
        }

        if (description.isBlank()) {
            ruleScore += 10;
            reasons.add("Job description is missing");
        } else if (description.length() < 50) {
            ruleScore += 10;
            reasons.add("Job description is unusually short");
        }

        if (requirements.isBlank()) {
            ruleScore += 5;
            reasons.add("Job requirements are missing");
        }

        if (benefits.isBlank()) {
            ruleScore += 3;
            reasons.add("Benefits information is missing");
        }

        if (location.isBlank()) {
            ruleScore += 5;
            reasons.add("Job location is missing");
        }

        // Upfront job-fee detection
        if (hasUpfrontPaymentRequest(allText)) {
            ruleScore += 35;
            reasons.add("Possible upfront job fee or deposit request detected");
        }

        // Sensitive credential requests
        if (containsAny(allText,
                "share your otp", "send your otp", "provide your otp",
                "share your password", "send your password",
                "share your upi pin", "provide your upi pin",
                "share your bank login", "send your bank login")) {
            ruleScore += 40;
            reasons.add("Request for sensitive credentials detected");
        }

        // Suspicious guarantees
        if (containsAny(allText,
                "guaranteed income", "guaranteed daily income",
                "earn lakhs daily", "earn money without working",
                "get rich quickly", "100% guaranteed job",
                "guaranteed job placement",
                "earn thousands daily with no experience")) {
            ruleScore += 15;
            reasons.add("Unusually strong earnings or job guarantees detected");
        }

        // Payment-linked hiring claims
        if (containsAny(allText,
                "job confirmed after payment",
                "pay to confirm your job",
                "pay to receive offer letter",
                "guaranteed selection after payment",
                "no interview required pay")) {
            ruleScore += 25;
            reasons.add("Possible payment-based hiring claim detected");
        }

        ruleScore = Math.min(ruleScore, 100);

        // Final decision
        String finalResult;

        if (mlResponse.getPrediction() == 1 || ruleScore >= 40) {
            finalResult = "FRAUDULENT";
        } else if (ruleScore >= 20) {
            finalResult = "SUSPICIOUS";
        } else {
            finalResult = "GENUINE";
        }

        if (reasons.isEmpty()) {
            reasons.add(
                    "No configured suspicious indicators were detected; "
                    + "this does not guarantee the job is genuine"
            );
        }

        VerificationResponse response = new VerificationResponse();
        response.setMlPrediction(mlResponse.getPrediction());
        response.setMlResult(mlResponse.getResult());
        response.setRuleScore(ruleScore);
        response.setFinalResult(finalResult);
        response.setReasons(reasons);

        return response;
    }

    private String getString(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return "";
    }

    private int getInt(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);

            if (value instanceof Number) {
                return ((Number) value).intValue();
            }

            if (value != null) {
                try {
                    return Integer.parseInt(value.toString().trim());
                } catch (NumberFormatException ignored) {
                    // Try the next supported key.
                }
            }
        }
        return 0;
    }

    private boolean hasLogoField(Map<String, Object> data) {
        return data.containsKey("has_company_logo")
                || data.containsKey("hasCompanyLogo");
    }

    private String normalize(String text) {
        return text == null ? ""
                : text.toLowerCase(Locale.ROOT)
                      .replaceAll("\\s+", " ")
                      .trim();
    }

    private boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (text.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUpfrontPaymentRequest(String text) {
        // First check for explicit statements that no fee is required.
        if (containsAny(text,
                "no registration fee", "no application fee",
                "no processing fee", "no upfront fee",
                "no payment required", "no fees required",
                "never pay a fee", "do not pay any fee",
                "don't pay any fee", "we do not charge a fee",
                "we never ask for payment")) {
            return false;
        }

        boolean paymentAction = containsAny(text,
                "pay", "payment", "transfer", "deposit",
                "send money", "submit payment", "wire money");

        boolean jobFee = containsAny(text,
                "registration fee", "application fee",
                "processing fee", "security deposit",
                "training fee", "interview fee",
                "job confirmation fee", "offer letter fee",
                "recruitment fee");

        return paymentAction && jobFee;
    }
}