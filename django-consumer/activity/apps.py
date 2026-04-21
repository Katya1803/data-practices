from django.apps import AppConfig


class ActivityConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'activity'

    def ready(self):
        import os
        # Only start in the main server process, not in the auto-reloader watcher
        if os.environ.get('RUN_MAIN') == 'true' or not os.environ.get('DJANGO_AUTORELOAD_ENV'):
            from .consumers import kafka_consumer
            kafka_consumer.start_in_background()
