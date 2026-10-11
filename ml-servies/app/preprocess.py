from __future__ import annotations

from pathlib import Path
from typing import Any

import joblib
import pandas as pd
from scipy.sparse import csr_matrix, hstack
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import OneHotEncoder

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = PROJECT_ROOT / "data"
MODEL_DIR = PROJECT_ROOT / "models"
MODEL_DIR.mkdir(exist_ok=True)

TEXT_COLUMNS = [
    "title",
    "company_profile",
    "description",
    "requirements",
    "benefits",
]
CATEGORICAL_COLUMNS = [
    "location",
    "department",
    "employment_type",
    "required_experience",
    "required_education",
    "industry",
    "function",
]
NUMERICAL_COLUMNS = ["telecommuting", "has_company_logo", "has_questions"]
SELECTED_FEATURES = [
    "title",
    "location",
    "department",
    "salary_range",
    "company_profile",
    "description",
    "requirements",
    "benefits",
    "telecommuting",
    "has_company_logo",
    "has_questions",
    "employment_type",
    "required_experience",
    "required_education",
    "industry",
    "function",
]


def _get_path(filename: str) -> Path:
    return MODEL_DIR / filename


def _prepare_feature_frame(df: pd.DataFrame) -> pd.DataFrame:
    fill_columns = [
        "location",
        "department",
        "salary_range",
        "company_profile",
        "description",
        "requirements",
        "benefits",
        "employment_type",
        "required_experience",
        "required_education",
        "industry",
        "function",
    ]
    prepared = df.copy()
    for column in fill_columns:
        prepared[column] = prepared[column].fillna("Unknown")
    return prepared[SELECTED_FEATURES]


def train_and_save_model() -> dict[str, Any]:
    dataset_path = DATA_DIR / "fake_job_postings.csv"
    if not dataset_path.exists():
        raise FileNotFoundError(f"Training dataset not found at {dataset_path}")

    df = pd.read_csv(dataset_path)
    prepared_df = _prepare_feature_frame(df)
    X = prepared_df.copy()
    y = df["fraudulent"]
    X["combined_text"] = X[TEXT_COLUMNS].astype(str).agg(" ".join, axis=1)

    tfidf = TfidfVectorizer(max_features=5000, stop_words="english")
    X_text = tfidf.fit_transform(X["combined_text"])

    encoder = OneHotEncoder(handle_unknown="ignore", sparse_output=True)
    X_categorical = encoder.fit_transform(X[CATEGORICAL_COLUMNS])

    X_numerical = X[NUMERICAL_COLUMNS].values
    X_numerical_sparse = csr_matrix(X_numerical)
    X_final = hstack([X_text, X_categorical, X_numerical_sparse])

    X_train, _, y_train, _ = train_test_split(
        X_final,
        y,
        test_size=0.20,
        random_state=42,
        stratify=y,
    )

    model = LogisticRegression(class_weight="balanced", max_iter=1000, random_state=42)
    model.fit(X_train, y_train)

    joblib.dump(model, _get_path("fraud_detection_model.pkl"))
    joblib.dump(tfidf, _get_path("tfidf_vectorizer.pkl"))
    joblib.dump(encoder, _get_path("onehot_encoder.pkl"))
    joblib.dump(NUMERICAL_COLUMNS, _get_path("numerical_columns.pkl"))
    joblib.dump(
        {
            "text_columns": TEXT_COLUMNS,
            "categorical_columns": CATEGORICAL_COLUMNS,
            "numerical_columns": NUMERICAL_COLUMNS,
            "selected_features": SELECTED_FEATURES,
        },
        _get_path("feature_config.pkl"),
    )

    return {
        "model": model,
        "tfidf": tfidf,
        "encoder": encoder,
        "feature_config": {
            "text_columns": TEXT_COLUMNS,
            "categorical_columns": CATEGORICAL_COLUMNS,
            "numerical_columns": NUMERICAL_COLUMNS,
            "selected_features": SELECTED_FEATURES,
        },
    }


def _load_artifacts() -> tuple[Any, Any, Any, dict[str, Any]]:
    model_path = _get_path("fraud_detection_model.pkl")
    tfidf_path = _get_path("tfidf_vectorizer.pkl")
    encoder_path = _get_path("onehot_encoder.pkl")
    feature_config_path = _get_path("feature_config.pkl")

    missing = [
        path.name for path in (model_path, tfidf_path, encoder_path, feature_config_path) if not path.exists()
    ]
    if missing:
        raise FileNotFoundError(
            "Missing model artifacts: " + ", ".join(missing) + ". Run train_and_save_model() first."
        )

    model = joblib.load(model_path)
    tfidf = joblib.load(tfidf_path)
    encoder = joblib.load(encoder_path)
    feature_config = joblib.load(feature_config_path)
    return model, tfidf, encoder, feature_config


def predict_job(job_data: pd.Series | dict[str, Any]) -> int:
    if isinstance(job_data, dict):
        job_data = pd.Series(job_data)

    model, tfidf, encoder, feature_config = _load_artifacts()
    prepared = _prepare_feature_frame(pd.DataFrame([job_data]))
    text_data = prepared[feature_config["text_columns"]].astype(str).agg(" ".join, axis=1)
    text_features = tfidf.transform(text_data)

    categorical_data = prepared[feature_config["categorical_columns"]]
    categorical_features = encoder.transform(categorical_data)

    numerical_values = prepared[feature_config["numerical_columns"]].astype(float).values.reshape(1, -1)
    numerical_features = csr_matrix(numerical_values)

    features = hstack([text_features, categorical_features, numerical_features])
    prediction = model.predict(features)
    return int(prediction[0])


if __name__ == "__main__":
    train_and_save_model()
    sample_job = pd.Series(
        {
            "title": "Software Engineer",
            "location": "Hyderabad, Telangana",
            "department": "Engineering",
            "salary_range": "5-8 LPA",
            "company_profile": "Established software company providing technology solutions.",
            "description": "We are looking for a software engineer to develop and maintain web applications.",
            "requirements": "Python, Java, SQL, problem solving and communication skills.",
            "benefits": "Health insurance, paid leave and professional development.",
            "telecommuting": 0,
            "has_company_logo": 1,
            "has_questions": 1,
            "employment_type": "Full-time",
            "required_experience": "Mid-Senior level",
            "required_education": "Bachelor's Degree",
            "industry": "Information Technology",
            "function": "Engineering",
        }
    )
    print("Sample prediction:", predict_job(sample_job))
