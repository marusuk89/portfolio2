from django.db import models
from users.models import UserAccount
from info.models import Building, Company
from contract.models import PartnerContract
from . import Issue
import datetime


class IssueReport(models.Model):
    class Meta:
        db_table = "manage_issue_report"

    issue = models.ForeignKey(Issue, on_delete=models.CASCADE)
    before_img_url1 = models.CharField(max_length=100, null=True)
    before_img_url2 = models.CharField(max_length=100, null=True)
    before_img_url3 = models.CharField(max_length=100, null=True)
    after_img_url1 = models.CharField(max_length=100, null=True)
    after_img_url2 = models.CharField(max_length=100, null=True)
    after_img_url3 = models.CharField(max_length=100, null=True)
    result = models.CharField(max_length=500, null=True)
    pdf_url = models.CharField(max_length=100, null=True)
    created_at = models.DateField(default=datetime.date.today)
    contract = models.ForeignKey(PartnerContract, on_delete=models.CASCADE, default=1, null=True)

    def __str__(self):
        return str(self.id)
