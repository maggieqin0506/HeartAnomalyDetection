FROM python:3.9-slim AS builder

WORKDIR /app

COPY requirements.txt .

COPY broker.py .

RUN pip install --no-cache-dir -r requirements.txt

FROM python:3.9-slim

WORKDIR /app

# Copy the built dependencies from the backend-builder stage
COPY --from=builder /usr/local/lib/python3.9/site-packages/ /usr/local/lib/python3.9/site-packages/

# Copy the application code from the backend-builder stage
COPY --from=builder /app /app

