# Generated by Django 4.1.1 on 2022-11-15 05:40

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("management", "0036_monthlyreportimage"),
    ]

    operations = [
        migrations.AddField(
            model_name="monthlyreportimage",
            name="month",
            field=models.IntegerField(default=1),
        ),
        migrations.AddField(
            model_name="monthlyreportimage",
            name="year",
            field=models.IntegerField(default=1),
        ),
    ]