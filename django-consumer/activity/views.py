from rest_framework.views import APIView
from rest_framework.response import Response
from .models import ActivityLog
from .serializers import ActivityLogSerializer


class ActivityLogListView(APIView):
    def get(self, request):
        customer_id = request.query_params.get('customerId')
        page = int(request.query_params.get('page', 1))
        page_size = int(request.query_params.get('page_size', 20))
        qs = ActivityLog.objects.all().order_by('-created_at')
        if customer_id:
            qs = qs.filter(customer_id=customer_id)
        total = qs.count()
        offset = (page - 1) * page_size
        results = qs[offset:offset + page_size]
        return Response({
            'count': total,
            'results': ActivityLogSerializer(results, many=True).data,
        })


class RecentActivityView(APIView):
    def get(self, request):
        logs = ActivityLog.objects.all().order_by('-created_at')[:20]
        return Response(ActivityLogSerializer(logs, many=True).data)
