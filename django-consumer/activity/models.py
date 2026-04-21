from django.db import models


class ActivityLog(models.Model):
    event_type = models.CharField(max_length=50)
    customer_id = models.IntegerField(null=True, blank=True)
    film_id = models.IntegerField(null=True, blank=True)
    rental_id = models.IntegerField(null=True, blank=True)
    payload = models.JSONField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.event_type} @ {self.created_at}"
