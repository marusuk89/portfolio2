# Generated by Django 4.1.1 on 2022-09-30 08:51

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("management", "0015_alter_tenantbill_paid_date"),
    ]

    operations = [
        migrations.AddField(
            model_name="tenantbill",
            name="whole_text",
            field=models.CharField(default="", max_length=500),
        ),
    ]