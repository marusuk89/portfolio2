# Generated by Django 4.1.1 on 2022-09-28 05:39

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("management", "0014_tenantbill_issued_date_alter_tenantbill_paid_date"),
    ]

    operations = [
        migrations.AlterField(
            model_name="tenantbill",
            name="paid_date",
            field=models.DateField(null=True),
        ),
    ]