# Generated by Django 4.1.1 on 2022-11-16 08:05

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("management", "0039_monthlyreport_month_monthlyreport_week_and_more"),
    ]

    operations = [
        migrations.AddField(
            model_name="monthlyreport",
            name="content",
            field=models.CharField(default="", max_length=500),
        ),
    ]
