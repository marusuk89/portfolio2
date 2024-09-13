# Generated by Django 4.1.1 on 2022-10-04 01:08

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ("contract", "0012_alter_buildingcontract_sales_person"),
        ("management", "0017_unknowntransaction"),
    ]

    operations = [
        migrations.AlterField(
            model_name="tenantbill",
            name="contract",
            field=models.ForeignKey(
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                to="contract.roomcontract",
            ),
        ),
    ]
