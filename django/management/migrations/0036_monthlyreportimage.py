# Generated by Django 4.1.1 on 2022-11-15 04:55

import datetime
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ("info", "0016_company_reg_number"),
        ("management", "0035_partnerbill_is_paid_partnerbill_issued_date_and_more"),
    ]

    operations = [
        migrations.CreateModel(
            name="MonthlyReportImage",
            fields=[
                (
                    "id",
                    models.BigAutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                ("title", models.CharField(default="", max_length=30)),
                ("comment", models.CharField(default="", max_length=500)),
                ("img_url1", models.CharField(default="", max_length=100, null=True)),
                ("img_url2", models.CharField(default="", max_length=100, null=True)),
                ("img_url3", models.CharField(default="", max_length=100, null=True)),
                ("img_url4", models.CharField(default="", max_length=100, null=True)),
                ("img_url5", models.CharField(default="", max_length=100, null=True)),
                ("week", models.IntegerField(default=1)),
                ("created_at", models.DateField(default=datetime.date.today)),
                ("uploaded_at", models.DateField(default=datetime.date.today)),
                (
                    "building",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE, to="info.building"
                    ),
                ),
                (
                    "partner",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE, to="info.company"
                    ),
                ),
            ],
            options={
                "db_table": "manage_monthly_report_image",
            },
        ),
    ]