# Generated by Django 4.0.2 on 2022-09-26 11:28

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('management', '0010_alter_tenantbill_paid_date'),
    ]

    operations = [
        migrations.AlterField(
            model_name='tenantbill',
            name='paid_date',
            field=models.DateField(auto_now_add=True),
        ),
    ]