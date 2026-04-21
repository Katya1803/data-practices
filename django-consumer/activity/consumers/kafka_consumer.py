import json
import threading
import logging
from confluent_kafka import Consumer, KafkaError
from django.conf import settings

logger = logging.getLogger(__name__)

_started = False


def handle_event(topic, data):
    # Import here to avoid AppRegistry issues
    from activity.models import ActivityLog
    ActivityLog.objects.create(
        event_type=topic,
        customer_id=data.get('customerId'),
        film_id=data.get('filmId'),
        rental_id=data.get('rentalId'),
        payload=data,
    )
    logger.info(f"Logged event: {topic} rental={data.get('rentalId')}")


def run_consumer():
    consumer = Consumer({
        'bootstrap.servers': settings.KAFKA_BOOTSTRAP_SERVERS,
        'group.id': settings.KAFKA_GROUP_ID,
        'auto.offset.reset': 'earliest',
    })
    consumer.subscribe(settings.KAFKA_TOPICS)
    logger.info("Kafka consumer started, listening on topics: %s", settings.KAFKA_TOPICS)

    try:
        while True:
            msg = consumer.poll(1.0)
            if msg is None:
                continue
            if msg.error():
                if msg.error().code() != KafkaError._PARTITION_EOF:
                    logger.error("Kafka error: %s", msg.error())
                continue
            try:
                data = json.loads(msg.value().decode('utf-8'))
                handle_event(msg.topic(), data)
            except Exception as e:
                logger.exception("Failed to process message: %s", e)
    finally:
        consumer.close()


def start_in_background():
    global _started
    if _started:
        return
    _started = True
    t = threading.Thread(target=run_consumer, daemon=True)
    t.start()
