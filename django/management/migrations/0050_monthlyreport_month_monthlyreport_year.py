# Generated by Django 4.0.6 on 2023-03-10 06:57

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('management', '0049_tenantbill_month_tenantbill_monthly_charge_and_more'),
    ]

    operations = [
        migrations.AddField(
            model_name='monthlyreport',
            name='month',
            field=models.IntegerField(default=1),
        ),
        migrations.AddField(
            model_name='monthlyreport',
            name='year',
            field=models.IntegerField(default=2023),
        ),
    ]