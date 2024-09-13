from django.db import models
import datetime


class Notice(models.Model):
    class Meta:
        db_table = "manage_notice"

    contents = models.CharField(max_length=500, default="")
    created_at = models.DateField(default=datetime.date.today)

    def __str__(self):
        return str(self.created_at)
