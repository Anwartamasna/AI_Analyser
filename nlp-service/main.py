import json
import os
import time
from kafka import KafkaConsumer, KafkaProducer
from resume_processor import ResumeProcessor
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Environment Variables
KAFKA_BOOTSTRAP_SERVERS = os.environ.get('KAFKA_BOOTSTRAP_SERVERS', 'kafka:9092')
REQUEST_TOPIC = os.environ.get('KAFKA_REQUEST_TOPIC', 'resume-analysis-request')
RESPONSE_TOPIC = os.environ.get('KAFKA_RESPONSE_TOPIC', 'resume-analysis-response')

def main():
    logger.info("Starting Python NLP Service...")
    logger.info(f"Connecting to Kafka at {KAFKA_BOOTSTRAP_SERVERS}")

    # Initialize Processor
    processor = ResumeProcessor()

    # Retry mechanism for Kafka connection
    consumer = None
    producer = None
    
    for i in range(10):
        try:
            consumer = KafkaConsumer(
                REQUEST_TOPIC,
                bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                group_id='nlp-service-group',
                auto_offset_reset='earliest'
            )
            producer = KafkaProducer(
                bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
                value_serializer=lambda v: json.dumps(v).encode('utf-8')
            )
            logger.info("Successfully connected to Kafka.")
            break
        except Exception as e:
            logger.warning(f"Failed to connect to Kafka (Attempt {i+1}/10): {e}")
            time.sleep(5)

    if not consumer or not producer:
        logger.error("Could not connect to Kafka after multiple retries. Exiting.")
        return

    logger.info(f"Listening on topic: {REQUEST_TOPIC}")

    for message in consumer:
        try:
            data = message.value
            logger.info(f"Received request for candidate_id: {data.get('candidate_id')}")
            
            resume_text = data.get('resume_text', '')
            job_description = data.get('job_description', '')
            candidate_id = data.get('candidate_id')

            if not resume_text or not job_description:
                logger.error("Missing resume_text or job_description")
                continue

            # Process
            analysis_result = processor.analyze(resume_text, job_description)
            
            # Prepare Response
            response = {
                "candidate_id": candidate_id,
                "analysis": analysis_result
            }

            # Send back to Kafka
            producer.send(RESPONSE_TOPIC, response)
            logger.info(f"Sent response for candidate_id: {candidate_id}")

        except Exception as e:
            logger.error(f"Error processing message: {e}")

if __name__ == "__main__":
    main()
