from django.contrib import admin

# Register your models here.

from .models.manage_notification import *

admin.site.register(Notification)