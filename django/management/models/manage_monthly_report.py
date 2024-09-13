from django.db import models
from users.models import UserAccount
from info.models.info_building import Building
from info.models.info_company import Company
from info.models import Building, Company
from contract.models import RoomContract, BuildingContract
import datetime


class MonthlyReport(models.Model):
    class Meta:
        db_table = "manage_monthly_report"

    agent = models.ForeignKey(
        UserAccount, on_delete=models.CASCADE, null=True
    )
    building = models.ForeignKey(BuildingContract, on_delete=models.CASCADE, null=True)
    title = models.CharField(max_length=40, default="")
    content = models.CharField(max_length=1000, default="")
    year = models.IntegerField(default=2023)
    month = models.IntegerField(default=1)
    createdAt = models.DateField(default=datetime.date.today)
    doc_url1 = models.CharField(max_length=100, default="", null=True)

    def __str__(self):
        return self.title