FROM python:3.12-alpine
RUN pip install mysql-connector-python redis
WORKDIR /app
COPY deploy/services/test/test_db.py /app/
USER 1000
CMD ["python", "/app/test_db.py"]
