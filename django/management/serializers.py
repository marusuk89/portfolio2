from rest_framework import serializers
from .models import Issue, MonthlyReport, IssueReport, Notice

class EstimateSerializer(serializers.Serializer):
    clean_selected = serializers.BooleanField(help_text="청소_선택", default=False)
    fire_selected = serializers.BooleanField(help_text="소방_선택", default=False)
    elvt_selected = serializers.BooleanField(help_text="엘베_선택", default=False)
    internet_selected = serializers.BooleanField(help_text="인터넷_선택", default=False)
    cctv_selected = serializers.BooleanField(help_text="CCTV_선택", default=False)
    clean_num_perweek = serializers.IntegerField(help_text="청소_횟수", default=1)
    elvt_manage_type = serializers.IntegerField(help_text="CCTV_패키지종류", default=0)
    cctv_cnt = serializers.IntegerField(help_text="CCTV_갯수", default=0)
    house_hold_cnt = serializers.IntegerField(help_text="세대수", default=1)
    total_floor = serializers.IntegerField(help_text="전체 층수", default=1)
    elvt_cnt = serializers.IntegerField(help_text="엘리베이터 개수", default=1)


class IssueSerializer(serializers.Serializer):
    id = serializers.IntegerField()
    building_id = serializers.IntegerField()
    building__address = serializers.CharField()
    building__name = serializers.CharField()
    room__tenant__phone_number = serializers.CharField(
        help_text="세입자전화번호", default="010-XXXX-XXXX"
    )
    room_number = serializers.CharField()
    category = serializers.CharField()
    description = serializers.CharField()
    request_date = serializers.DateField()
    is_emergency = serializers.BooleanField()
    is_handled = serializers.BooleanField()
    img_url1 = serializers.CharField()
    img_url2 = serializers.CharField()
    img_url3 = serializers.CharField()
    img_url4 = serializers.CharField()
    issuer__id = serializers.CharField()
    is_issuer = serializers.CharField()


class ReportSerializer(serializers.ModelSerializer):
    class Meta:
        model = MonthlyReport
        fields = "__all__"


class IssueReportSerializer(serializers.ModelSerializer):
    class Meta:
        model = IssueReport
        fields = "__all__"


class NoticeSerializer(serializers.ModelSerializer):
    class Meta:
        model = Notice
        fields = "__all__"

class IsHandledSerializer(serializers.Serializer):
    is_handled=serializers.CharField(help_text="True, False, Both")

class PaymentSuccessSerializer(serializers.Serializer):
    paymentKey = serializers.CharField()
    orderId = serializers.CharField()
    amount = serializers.DictField()
    # method = serializers.CharField()
    # cardInfo = serializers.DictField()
    # buyer = serializers.DictField()