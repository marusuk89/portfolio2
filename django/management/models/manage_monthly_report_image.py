from django.db import models
from users.models import UserAccount
from info.models.info_building import Building
from info.models.info_company import Company
from info.models import Building, Company
import datetime


class MonthlyReportImage(models.Model):
    class Meta:
        db_table = "manage_monthly_report_image"

    partner = models.ForeignKey(Company, on_delete=models.CASCADE)
    building = models.ForeignKey(Building, on_delete=models.CASCADE)
    title = models.CharField(max_length=30, default="")
    comment = models.CharField(max_length=500, default="")
    img_url1 = models.CharField(max_length=100, default="", null=True)
    img_url2 = models.CharField(max_length=100, default="", null=True)
    img_url3 = models.CharField(max_length=100, default="", null=True)
    img_url4 = models.CharField(max_length=100, default="", null=True)
    img_url5 = models.CharField(max_length=100, default="", null=True)
    year = models.IntegerField(default=1)
    month = models.IntegerField(default=1)
    week = models.IntegerField(default=1)
    created_at = models.DateField(default=datetime.date.today)
    uploaded_at = models.DateField(default=datetime.date.today)

    def __str__(self):
        return self.title
