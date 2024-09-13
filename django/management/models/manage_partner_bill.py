from django.db import models
from users.models import UserAccount
from info.models.info_company import Company
from contract.models.contract_partner import PartnerContract
import datetime


class PartnerBill(models.Model):
    class Meta:
        db_table = "manage_partner_bill"

    contract = models.ForeignKey(PartnerContract, on_delete=models.CASCADE)
    issued_date = models.DateField(auto_now=True)
    paid_date = models.DateField(null=True)
    paid_amount = models.IntegerField(default=0)
    is_paid = models.BooleanField(default=False)
    whole_text = models.CharField(max_length=500, default="")
    monthly_charge = models.IntegerField(default=0)

    def __str__(self):
        return str(self.contract)
