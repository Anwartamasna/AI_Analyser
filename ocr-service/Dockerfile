FROM python:3.9-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    tesseract-ocr \
    libtesseract-dev \
    poppler-utils \
    && rm -rf /var/lib/apt/lists/*

# Install heavy dependencies first (better Docker layer caching)
# These rarely change, so this layer will be cached
COPY requirements-heavy.txt .
RUN pip install --no-cache-dir --prefer-binary -r requirements-heavy.txt

# Download smaller Spacy model (12MB vs 40MB)
RUN python -m spacy download en_core_web_sm

# Install lighter dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir --prefer-binary -r requirements.txt

COPY . .

CMD ["python", "main.py"]
