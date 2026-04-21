from django.urls import path
from . import views

urlpatterns = [
    path('activity-log', views.ActivityLogListView.as_view()),
    path('activity-log/recent', views.RecentActivityView.as_view()),
]
