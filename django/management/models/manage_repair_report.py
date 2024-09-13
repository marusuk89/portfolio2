from unicodedata import category
from django.db import models
from users.models import UserAccount
from info.models.info_building import Building
from contract.models import RoomContract, BuildingContract
import datetime


class RepairReport(models.Model):
    class Meta:
        db_table = "manage_repair_report"

    agent = models.ForeignKey(
        UserAccount, on_delete=models.CASCADE, null=True
    )
    building = models.ForeignKey(BuildingContract, on_delete=models.CASCADE, null=True)
    title = models.CharField(max_length=40, default="")
    content = models.CharField(max_length=1000, default="")
    createdAt = models.DateField(default=datetime.date.today)
    img_url1 = models.CharField(max_length=100, default="", null=True)
    img_url2 = models.CharField(max_length=100, default="", null=True)
    img_url3 = models.CharField(max_length=100, default="", null=True)

    def __str__(self):
        return self.agent
