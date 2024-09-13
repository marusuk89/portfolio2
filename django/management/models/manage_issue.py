from unicodedata import category
from django.db import models
from users.models import UserAccount
from info.models.info_building import Building
from contract.models import RoomContract
import datetime


class Issue(models.Model):
    class Meta:
        db_table = "manage_issue"

    issuer = models.ForeignKey(UserAccount, on_delete = models.CASCADE, default=1)
    building = models.ForeignKey(Building, on_delete=models.CASCADE)
    room = models.ForeignKey(RoomContract, on_delete=models.CASCADE, null=True)
    room_number = models.CharField(max_length=10, default="", null=True)
    category = models.CharField(max_length=50, default="")
    description = models.CharField(max_length=500, default="")
    request_date = models.DateField(default=datetime.date.today)
    is_emergency = models.BooleanField(default=False)
    is_handled = models.BooleanField(default=False)
    img_url1 = models.CharField(max_length=100, default="", null=True)
    img_url2 = models.CharField(max_length=100, default="", null=True)
    img_url3 = models.CharField(max_length=100, default="", null=True)
    img_url4 = models.CharField(max_length=100, default="", null=True)
    img_url5 = models.CharField(max_length=100, default="", null=True)

    def __str__(self):
        return self.room_number
