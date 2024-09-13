from django.urls import path, include
from . import views

urlpatterns = [
    path("notice", views.NoticeList.as_view()),
    
    path("notification", views.NotificationList.as_view()),
    path("notification/<str:category>", views.NotifiactinoDelete.as_view()),

    path(
        "notification_read/<int:notifiaction_id>", views.NotifiactionMarkRead.as_view()
    ),
    
    path(
        "tenant_bill/<int:building_id>/<int:year>/<int:month>",
        views.TenantBillList.as_view(),
    ),
    path(
        "tenant_bill_for_tenant/<int:year>/<int:month>",
        views.TenantBillForTenant.as_view(),
    ),
    path("tenant_entire_bill", views.TenantBillEntireList.as_view()),
    
    path(
        "partner_bill/<int:building_id>/<int:year>/<int:month>",
        views.PartnerBillList.as_view(),
    ),
    path("partner_entire_bill", views.PartnerBillEntireList.as_view()),

    path("car_search/<int:building_id>/<int:car_number>", views.CarSearch.as_view()),
    path("car_search_list/<int:building_id>", views.CarSearchList.as_view()),
    
    
    path("monthly_reports", views.MonthlyReportList.as_view()),
    path("monthly_report_post", views.MonthlyReportPost.as_view()),
    path("monthly_report_update/<str:type>/<int:id>", views.MonthlyReportUpdate.as_view()),
    path(
        "monthly_report_image_post",
        views.MonthlyReportImagePost.as_view(),
    ),
    path(
        "monthly_report_by_time/<int:year>/<int:month>",
        views.MonthlyReportListByTime.as_view(),
    ),
    path(
        "monthly_report_by_building/<int:year>/<int:month>/<int:building_id>/<str:category>",
        views.MonthlyReportListByTimeAndBuilding.as_view(),
    ),
    
    
    path("issue/<int:issue_id>", views.IssueInfo.as_view()),
    path("issues", views.IssueList.as_view()),
    path("issue_report_update/<int:issue_report_id>", views.IssueReportUpdate.as_view()),
    path("issue_by_building/<int:building_id>", views.IssueListByBuilding.as_view()),
    path("issue_report_list", views.IssueReportList.as_view()),
    path(
        "issue_report/<int:building_id>/<int:issue_id>", views.IssueReportInfo.as_view()
    ),
    path("issue_reports/<int:building_id>", views.IssueReportListByBuilding.as_view()),
    
    path("rate/<int:issue_report_id>", views.RatePartner.as_view()),
    path("myrates", views.MyRatePartner.as_view()),
    
    path("category/<int:building_id>", views.IssueAvailableCategory.as_view()),

    path("carlistread/<int:building_id>/<str:car_number>", views.CarListRead ),

    path("repairreportcreate/<int:building_id>/<int:repair_report_id>", views.RepairReportCreate ),
    path("repairreportread/<int:building_id>", views.RepairReportRead ),
    path("repairreportdelete/<int:building_id>/<int:repair_report_id>", views.RepairReportDelete ),

    path("monthlyreportcreate/<int:building_id>/<int:monthly_report_id>/<int:year>/<int:month>", views.MonthlyReportCreate ),
    path("monthlyreportread/<int:building_id>/<int:year>", views.MonthlyReportRead ),
    path("monthlyreportdelete/<int:building_id>/<int:monthly_report_id>", views.MonthlyReportDelete ),

    path("tenantbillreadbymonthone/<int:year>/<int:month>", views.TenantBillReadByMonthOne ),
    path("tenantbillreadnotpaid/<int:building_id>", views.TenantBillReadNotPaid ),

    #결제
    path("payment_confirm", views.payment_confirm ),

    path("tenantbillreadlist/<int:building_id>", views.TenantBillReadList ),
    path("tenantbillreadbyroom/<int:year>/<int:room_id>", views.TenantBillReadByRoom ),

    path('payment_success', views.PaymentSuccess),
    path('payment_success', views.PaymentSuccess),
]
