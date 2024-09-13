# Generated by Django 4.1.1 on 2022-11-15 04:48

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("management", "0034_issue_issuer"),
    ]

    operations = [
        migrations.AddField(
            model_name="partnerbill",
            name="is_paid",
            field=models.BooleanField(default=False),
        ),
        migrations.AddField(
            model_name="partnerbill",
            name="issued_date",
            field=models.DateField(auto_now=True),
        ),
        migrations.AddField(
            model_name="partnerbill",
            name="paid_amount",
            field=models.IntegerField(default=0),
        ),
        migrations.AddField(
            model_name="partnerbill",
            name="whole_text",
            field=models.CharField(default="", max_length=500),
        ),
        migrations.AddField(
            model_name="tenantbill",
            name="monthly_charge",
            field=models.IntegerField(default=0),
        ),
    ]