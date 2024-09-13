from django.db import models
from users.models import UserAccount
from info.models.info_company import Company
from contract.models import RoomContract
from datetime import datetime
from django.utils import timezone


class WholeTenantBill(models.Model):
    class Meta:
        db_table = "manage_whole_tenant_bill"

    name = models.CharField(max_length=500, default="")
    paid_date_time = models.DateTimeField(auto_now=True)
    paid_amount = models.IntegerField(default=0)
    whole_text = models.CharField(max_length=500, default="")    

    def __str__(self):
        return str(self.name)
