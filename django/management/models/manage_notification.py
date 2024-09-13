from django.db import models
import datetime
from users.models import UserAccount
from management.models import IssueReport, Issue, MonthlyReport

class Notification(models.Model):
    class Meta:
        db_table = "manage_notification"

    sender = models.ForeignKey(
        UserAccount, on_delete=models.CASCADE, related_name="sender", null=True
    )
    recipient = models.ForeignKey(
        UserAccount, on_delete=models.CASCADE, related_name="recipient"
    )
    is_read = models.BooleanField(default=False)
    issue_report = models.ForeignKey(
        IssueReport, on_delete=models.CASCADE,null=True
    )
    issue = models.ForeignKey(
        Issue, on_delete=models.CASCADE,null=True
    )
    monthly_report = models.ForeignKey(
        MonthlyReport, on_delete=models.CASCADE,null=True
    )
    category = models.CharField(max_length=20, default="")
    contents = models.CharField(max_length=500, default="")
    created_at = models.DateField(default=datetime.date.today)
    def __str__(self):
        return str(self.recipient)
