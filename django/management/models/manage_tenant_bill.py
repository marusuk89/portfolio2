from django.db import models
from users.models import UserAccount
from info.models.info_company import Company
from contract.models import RoomContract
from datetime import datetime
from django.utils import timezone


class TenantBill(models.Model):
    class Meta:
        db_table = "manage_tenant_bill"

    contract = models.ForeignKey(RoomContract, on_delete=models.CASCADE, null=True)
    issued_date = models.DateField(auto_now=True)
    paid_date = models.DateField(null=True)
    is_paid = models.BooleanField(default=False)
    whole_text = models.CharField(max_length=500, default="")
    monthly_charge = models.IntegerField(default=0)
    year = models.IntegerField(default = 2023)
    month = models.IntegerField(default = 3)
    
    def __str__(self):
        return str(self.contract)
