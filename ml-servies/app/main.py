
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import pandas as pd
import sys
import os

# Add app folder to Python path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from preprocess import predict_job

app = FastAPI(
    title="Job & Internship Fraud Detection API",
    description="Machine Learning API for detecting fraudulent job and internship postings",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class JobRequest(BaseModel):
    title: str
    location: str
    department: str
    salary_range: str
    company_profile: str
    description: str
    requirements: str
    benefits: str
    telecommuting: int
    has_company_logo: int
    has_questions: int
    employment_type: str
    required_experience: str
    required_education: str
    industry: str
    function: str


@app.get("/")
def home():
    return {
        "service": "Job & Internship Fraud Detection ML Service",
        "status": "running",
        "version": "1.0.0"
    }


@app.get("/health")
def health():
    return {
        "service": "fraud-detection-ml-service",
        "status": "healthy",
        "model": "Linear SVM"
    }


@app.post("/predict")
def predict(job: JobRequest):
    job_data = pd.Series(job.model_dump())
    prediction = predict_job(job_data)

    reasons = []

    if not job.company_profile.strip():
        reasons.append("Company profile is missing")
    if job.has_company_logo == 0:
        reasons.append("Company logo is not available")
    if not job.salary_range.strip():
        reasons.append("Salary information is missing")
    if not job.requirements.strip():
        reasons.append("Job requirements are missing")
    if not job.benefits.strip():
        reasons.append("Benefits information is missing")
    if not job.location.strip():
        reasons.append("Job location is missing")
    if not job.description.strip() or len(job.description.strip()) < 50:
        reasons.append("Job description is very short")
    if job.has_questions == 0:
        reasons.append("Job posting does not contain screening questions")

    if prediction == 1:
        result = "FRAUDULENT"
        risk_score = min(90 + len(reasons) * 2, 100)
    else:
        result = "GENUINE"
        risk_score = min(len(reasons) * 5, 50)

    if risk_score <= 20:
        risk_level = "LOW"
    elif risk_score <= 50:
        risk_level = "MEDIUM"
    elif risk_score <= 75:
        risk_level = "HIGH"
    else:
        risk_level = "CRITICAL"

    if not reasons:
        reasons.append("No major suspicious indicators detected")

    return {
        "status": "success",
        "prediction": int(prediction),
        "result": result,
        "risk_score": risk_score,
        "risk_level": risk_level,
        "reasons": reasons,
        "model": "Linear SVM"
    }