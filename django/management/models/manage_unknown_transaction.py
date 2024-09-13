from django.db import models
from users.models import UserAccount
from info.models.info_company import Company
from contract.models import RoomContract
from datetime import datetime
from django.utils import timezone


class UnknownTransaction(models.Model):
    class Meta:
        db_table = "manage_unknown_transaction"

    name = models.CharField(max_length=500, default="")
    paid_amount = models.IntegerField(default=0)
    paid_date = models.DateField(auto_now=True)
    whole_text = models.CharField(max_length=500, default="")

    def __str__(self):
        return name
