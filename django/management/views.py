from rest_framework.views import APIView
from rest_framework.response import Response
from users.models import UserAccount
from django.core.paginator import Paginator
from django.db.models import Count, F
from contract.models import RoomContract, PartnerContract, BuildingContract
from drf_yasg.utils import swagger_auto_schema
from info.models import Building, Company, CompanyRate, BillLog
from .models import RepairReport, MonthlyReport, TenantBill
from .serializers import IssueSerializer, NoticeSerializer
from info.serializers import CompanyRateSerializer
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from users.serializers import pageSerializer
from . import models, serializers
from api.util import util
import json
import datetime
from datetime import date
import random
import string
from django.shortcuts import redirect
import http.client
from django.http import JsonResponse
import base64
from .serializers import PaymentSuccessSerializer


# import toss_payments
# from background_task import background

def generate_filename(local_file):
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"{timestamp}_{local_file.name}"
        return filename

def generate_random_filename(local_file):
        random_string = ''.join(random.choices(string.ascii_uppercase + string.ascii_lowercase + string.digits, k=5))
        filename = f"{random_string}_{local_file}"
        return filename

class TenantBillForTenant(APIView):
    def get(self, request, year, month):
        user = request.user

        if not user.category == "세입자":
            return Response({"Failed": "Invalid user type"})

        contrcat_info = RoomContract.objects.get(tenant=user)

        due_date = util.set_due_date2(contrcat_info.paying_date, month, year)

        paid_info = models.TenantBill.objects.get(
            contract__tenant=user.id,
            issued_date__year=year,
            issued_date__month=month,
        )

        return Response(
            {
                "data": {
                    "due_date": due_date,
                    "monthly_charge": contrcat_info.monthly_charge,
                    "bank": user.bank,
                    "account_number": user.account_number,
                    "depositor": user.depositor,
                    "is_paid":  paid_info.is_paid,
                    "paid_date": paid_info.paid_date,
                    "paid_amount": paid_info.paid_amount,
                }
            }
        )

        paid_info = models.TenantBill.objects.get(
            contract__tenant=user.id,
            issued_date__year=year,
            issued_date__month=month,
        )
        
        return Response(
            {
                "data": {
                    "due_date": due_date,
                    "monthly_charge": contrcat_info.monthly_charge,
                    "bank": user.bank,
                    "account_number": user.account_number,
                    "depositor": user.depositor,
                    "is_paid":  paid_info.is_paid,
                    "paid_date": paid_info.paid_date,
                    "paid_amount": paid_info.paid_amount,
                }
            }
        )

class TenantBillList(APIView):
    def get(self, request, building_id, year, month):
        user = request.user
        result = []
        building_ids = util.getBIds4AOR(user)

        if user.category == "세입자":
            contrcat_info = RoomContract.objects.get(tenant=user)
            
            due_date = util.set_due_date(contrcat_info.paying_date)

            paid_info = models.TenantBill.objects.get(
                contract__tenant=user.id,
                issued_date__year=year,
                issued_date__month=month,
            )
            
            return Response(
                {
                    "data": {
                        "due_date": due_date,
                        "monthly_charge": contrcat_info.monthly_charge,
                        "bank": user.bank,
                        "account_number": user.account_number,
                        "depositor": user.depositor,
                        "is_paid":  paid_info.is_paid,
                        "paid_date": paid_info.paid_date,
                        "paid_amount": paid_info.paid_amount,
                    }
                }
            )
        else:
            bills = (
                models.TenantBill.objects.filter(
                    contract__building=building_id,
                    issued_date__year=year,
                    issued_date__month=month,
                )
                .annotate(room_number=F("contract__room_number"), paid=F("is_paid"))
                .order_by("room_number")
                .values("room_number", "paid")
            )
            return Response({"data": bills})


class PartnerBillList(APIView):
    def get(self, request, building_id, year, month):
        user = request.user
        building_ids = util.getBIds4AORT(user)

        if building_id in building_ids:
            paid_partners = models.PartnerBill.objects.filter(
                contract__building=building_id,
                paid_date__year=year,
                paid_date__month=month,
            )
        else:
            return Response({"Failed": "Invalid Building Id"})

        partnerbill_infos = models.PartnerBill.objects.filter(
            contract__building=building_id,  
            paid_date__year=year,
            paid_date__month=month,).values(
            "contract__partner__id",
            "contract__partner__category",
            "contract__partner__name",
            "contract__partner__address",
            "contract__partner__phone_number",
            "contract__partner__account_holder",
            "contract__partner__account_number",
            "contract__monthly_charge",
            "paid_date",
            "is_paid",
        )
        return Response({"data": partnerbill_infos})


class IssueList(APIView):
    @swagger_auto_schema(query_serializer=pageSerializer)
    def get(self, request):
        user = request.user
        building_ids = util.getBIds4AOPRT(user)
        if user.category == "파트너":
            category = Company.objects.get(user=user).category

            issues = models.Issue.objects.filter(
                building__in=building_ids, category=category, is_handled=False
            ).values().order_by("id").annotate(
                room__tenant__phone_number=F("room__tenant__phone_number"), 
                building__address=F("building__address"), 
                building__name=F("building__name"), 
                issuer__id=F("issuer__id"),
                ) 
        else:
            issues = models.Issue.objects.filter(
                building__in=building_ids, is_handled=False
                ).values().order_by("id").annotate(
                room__tenant__phone_number=F("room__tenant__phone_number"), 
                building__address=F("building__address"), 
                building__name=F("building__name"), 
                issuer__id=F("issuer__id"),
            ) 
        for issue in issues:
            issue["is_issuer"] = issue["issuer__id"]==user.id
        page = request.GET.get("page")
        paginator = Paginator(issues, 10)
        pages = paginator.get_page(page)

        pages = IssueSerializer(pages, many=True).data
        return Response({"data": pages, "numpages": paginator.num_pages})


class IssueListByBuilding(APIView):
    @swagger_auto_schema(query_serializer=serializers.IsHandledSerializer)
    def get(self, request, building_id):
        user = request.user
        is_handled=request.GET.get("is_handled")

        if not user.is_authenticated:
            return Response({"Failed":"Not loged on"})
        if not util.validateBuildingId(user, building_id):
            return Response({"Failed": "Invalid Building ID"})
        
        if is_handled=="Both":
            issues = models.Issue.objects.filter(building=building_id)
        
        else:
            issues = models.Issue.objects.filter(building=building_id, is_handled=is_handled)
                    
        issues = issues.values().order_by("-request_date"
                    ).annotate( 
                    building__name=F("building__name"),
                    building__address=F("building__address"),
                    room__tenant__phone_number=F("room__tenant__phone_number"),
                    room__owner__phone_number=F("room__owner__phone_number"),
                    issuer__id=F("issuer__id"),
                    issuer__phone_number=F("issuer__phone_number"),
                    issuer__category=F("issuer__category"),
                    issuer__room_contract_tenant__room_number=F("issuer__room_contract_tenant__room_number"),
                )
        for issue in issues:
            issue["issuer__room_contract_owner__room_number"] = RoomContract.objects.filter(
                    owner=issue["issuer__id"]).values_list("room_number", flat=True)[::1]
            issue["is_issuer"] = issue["issuer__id"]==user.id
            if issue["category"] !="기타":
                issue["partner_id"] = PartnerContract.objects.filter(
                    building=issue["building_id"], partner__category=issue["category"]
                    ).values_list("partner__id", flat=True)[::1][0]
            else:
                issue["partner_id"] =""
            
            issue["img_urls"] =[]
            if issue["img_url1"]:
                issue["img_urls"].append(issue["img_url1"])
            if issue["img_url2"]:
                issue["img_urls"].append(issue["img_url2"]) 
            if issue["img_url3"]:
                issue["img_urls"].append(issue["img_url3"]) 
            if issue["img_url4"]:
                issue["img_urls"].append(issue["img_url4"]) 
            if issue["img_url5"]:
                issue["img_urls"].append(issue["img_url5"]) 
                
            
        return Response({"data": issues})


    @swagger_auto_schema(request_body=serializers.IssueSerializer)
    def post(self, request, building_id):
        user = request.user
        
        if not util.validateBuildingId(user, building_id):
            return Response({"Failed": "Invalid Building ID"})
        
        data = request.POST.get("data")
        img_files = request.FILES.getlist("img")
        data = json.loads(data)
        temp = {}
        room_number = data.get("room_number")
        if not room_number=="공동구역":
            room_id = RoomContract.objects.filter(
                building_id=building_id, 
                room_number=room_number
                ).values_list("id", flat=True)[::1][0]
            temp["room"] = RoomContract(id=room_id)

        temp["room_number"] = room_number
        temp["category"] = data.get("category")
        temp["description"] = data.get("description")
        temp["is_emergency"] = data.get("is_emergency") or False
        temp["building"] = Building(id=building_id)
        temp["issuer"] = UserAccount(id=user.id)

        for idx, f in enumerate(img_files):
            temp["img_url" + str(idx + 1)] = f.name
            util.upload_to_aws(f)

        issue_id = models.Issue.objects.create(**temp)
        
        partner_ids = (
            PartnerContract.objects.filter(building=building_id)
            .values_list("partner__user__id", flat=True)
            .order_by("partner__user__id")
            .distinct()
        )[::1]
        owner_ids = (
            RoomContract.objects.filter(building=building_id)
            .values_list("owner__id", flat=True)
            .order_by("owner__id")
            .distinct()
        )[::1]
        owner_represent_ids = (
            BuildingContract.objects.filter(building=building_id)
            .values_list("owner_represent__id", flat=True)
            .order_by("owner_represent__id")
            .distinct()
        )[::1]
        
        tenant_ids = (
            RoomContract.objects.filter(building=building_id)
            .values_list("tenant__id", flat=True)
            .order_by("tenant__id")
            .distinct()
        )[::1]
        
        recipient_ids = list(set(partner_ids + owner_ids + owner_represent_ids + tenant_ids))
        recipient_ids = [i for i in recipient_ids if i is not None]
        
        building_name = Building.objects.get(id=building_id).name
        
        for recipient_id in recipient_ids:
            recipient = UserAccount(id=recipient_id)
            models.Notification.objects.create(
                recipient=recipient,
                contents=f"{building_name}빌딩 {room_number}에서 하자가 보고 되었습니다.",
                issue = issue_id,
                category ="issue",
            )

        return Response({"Success": "Issue has been successfully posted"})
    


class IssueInfo(APIView):
    def get(self, reqeust, issue_id):
        issue = models.Issue.objects.filter(id=issue_id).values(
            "id",
            "building__id",
            "building__name",
            "building__address",
            "room_number",
            "category",
            "description",
            "img_url1",
            "img_url2",
            "img_url3",
            "img_url4",
            "issuer",
        )[0]
        return Response({"data": issue})

class IssueAvailableCategory(APIView):
    def get(self, request, building_id):
        user = request.user
        # if not util.validateBuildingId(user, building_id):
        #     return Resepont({"Failed":"Invalid building id"})
        categories = PartnerContract.objects.filter(
            building=building_id
            ).values_list("partner__category", flat=True
            ).order_by("partner__category").distinct()
        return Response({"data":categories})



class IssueReportList(APIView):
    def get(self, request):
        user = request.user
        building_ids = util.getBIds4AOPRT(user)

        if user.category == "파트너":
            issue_reports = models.IssueReport.objects.filter(
            issue__building__in=building_ids,
            contract__partner__user=user.id
        ).values().annotate(
                    issue__room__room_number=F("issue__room__room_number"),
                    issue__building__id=F("issue__building__id"),
                    issue__building__name=F("issue__building__name"),
                    issue__category=F("issue__category"),
                    issue__issuer=F("issue__issuer"),
                    contract__partner__name=F("contract__partner__name"),
                    contract__partner__address=F("contract__partner__address")
                    )
        
        else:
            issue_reports = models.IssueReport.objects.filter(
                issue__building__in=building_ids
            ).values().annotate(
                    issue__room__room_number=F("issue__room__room_number"),
                    issue__building__id=F("issue__building__id"),
                    issue__building__name=F("issue__building__name"),
                    issue__category=F("issue__category"),
                    issue__issuer=F("issue__issuer"),
                    contract__partner__name=F("contract__partner__name"),
                    contract__partner__address=F("contract__partner__address")
                    )
        for issue_report in issue_reports:
            issue_report["before_img_urls"] =[]
            issue_report["after_img_urls"] =[]

            if issue_report["before_img_url1"]:
                issue_report["before_img_urls"].append(issue_report["before_img_url1"])
            if issue_report["before_img_url2"]:
                issue_report["before_img_urls"].append(issue_report["before_img_url2"]) 
            if issue_report["before_img_url3"]:
                issue_report["before_img_urls"].append(issue_report["before_img_url3"]) 
            if issue_report["after_img_url1"]:
                issue_report["after_img_urls"].append(issue_report["after_img_url1"])
            if issue_report["after_img_url2"]:
                issue_report["after_img_urls"].append(issue_report["after_img_url2"]) 
            if issue_report["after_img_url3"]:
                issue_report["after_img_urls"].append(issue_report["after_img_url3"]) 

        return Response({"data": issue_reports})


class IssueReportListByBuilding(APIView):
    def get(self, request, building_id):
        user = request.user
        if not util.validateBuildingId(user, building_id):
            return Response({"Failed": "Invalid Building ID"})

        issue_reports = models.IssueReport.objects.filter(
            issue__building=building_id
        ).values(
            "id",
            "created_at", 
            "issue__building__id", 
            "issue__building__name", 
            "issue__building__address_new", 
            "issue__issuer__id",)
        
        for issue_report in issue_reports:
            issue_report["is_issuer"] = issue_report["issue__issuer__id"]==user.id
        
        return Response({"data": issue_reports})

        
            

class IssueReportInfo(APIView):
    def get(self, request, building_id, issue_id):
        user = request.user
        if not models.IssueReport.objects.filter(issue=issue_id).exists():
            return Response({"Failed":"Invalid issue ID"})
        issue_reports = models.IssueReport.objects.filter(issue=issue_id).values()
        for issue_report in issue_reports:
            issue_report["before_img_urls"] =[]
            issue_report["after_img_urls"] =[]
            if issue_report["before_img_url1"]:
                issue_report["before_img_urls"].append(issue_report["before_img_url1"])
            if issue_report["before_img_url2"]:
                issue_report["before_img_urls"].append(issue_report["before_img_url2"]) 
            if issue_report["before_img_url3"]:
                issue_report["before_img_urls"].append(issue_report["before_img_url3"]) 
            if issue_report["after_img_url1"]:
                issue_report["after_img_urls"].append(issue_report["after_img_url1"])
            if issue_report["after_img_url2"]:
                issue_report["after_img_urls"].append(issue_report["after_img_url2"]) 
            if issue_report["after_img_url3"]:
                issue_report["after_img_urls"].append(issue_report["after_img_url3"])
        return Response({"data":issue_reports[0]})

    @swagger_auto_schema(request_body=serializers.IssueReportSerializer)
    def post(self, request, building_id, issue_id):
        user =request.user
        if user.category !="파트너":
            return Response({"Failed":"Invalid user type"})
        
        if models.IssueReport.objects.filter(issue=issue_id).exists():
            return Response({"Failed":"Report is already uploaded"})
        
        if  not models.Issue.objects.filter(id=issue_id).exists():
            return Response({"Failed":"Invalid issue id"})  
        result = request.POST.get("result")
        before_img_files = request.FILES.getlist("before")
        after_img_files = request.FILES.getlist("after")
        
        useraccount = UserAccount(id=request.user.id)
        partner = Company(user=useraccount)
        issue = models.Issue(id=issue_id)
        contract = PartnerContract.objects.get(partner__user=useraccount)

        temp = {
            "issue":issue,
            "contract": contract,
            "result":result
        }

        for idx, f in enumerate(before_img_files):
            temp["before_img_url" + str(idx + 1)] = f.name
            util.upload_to_aws(f)
        
        for idx, f in enumerate(after_img_files):
            temp["after_img_url" + str(idx + 1)] = f.name
            util.upload_to_aws(f)
        
        issue_report = models.IssueReport.objects.create(**temp)
        models.Issue.objects.filter(id=issue_id).update(is_handled=True)
        models.Notification.objects.create(
            recipient=issue.issuer, 
            contents="접수된 하자가 수리되었습니다.", 
            issue_report=issue_report,
            category = "issue_report",
        )
        return Response({"Success": "Issue Report is successfully uploaded"})

class IssueReportUpdate(APIView):
    def post(self, request, issue_report_id):
        user = request.user
        
        if not user.category=="파트너":
            return Response({"Failed":"Invalid user type"})
        if not models.IssueReport.objects.filter(id=issue_report_id).exists():
            return Response({"Failed":"Invalid issue report id"})
        result = request.POST.get("result")
        before_img_files = request.FILES.getlist("before")
        after_img_files = request.FILES.getlist("after")
        temp = {"result":result}
        print(result)
        print(before_img_files[0].name)
        
        for i in range(0,3):
            temp["before_img_url" + str(i + 1)] =""
            temp["after_img_url" + str(i + 1)] =""
            
        for idx, f in enumerate(before_img_files):
            temp["before_img_url" + str(idx + 1)] = f.name
            util.upload_to_aws(f)
        
        for idx, f in enumerate(after_img_files):
            temp["after_img_url" + str(idx + 1)] = f.name
            util.upload_to_aws(f)
        
        models.IssueReport.objects.filter(id=issue_report_id).update(**temp)
        
        return Response({})


class MonthlyReportList(APIView):
    def get(self, request):
        user = request.user
        
        if not user.category=="파트너":
            return Response({"Failed":"Invalid user type"})
        
        pdf_reports = models.MonthlyReport.objects.filter(
            partner__user=user.id).annotate(
                building__name=F("building__name"),
                building__address_new=F("building__address_new")).values()
        img_reports = models.MonthlyReportImage.objects.filter(
            partner__user=user.id,).annotate(
                building__name=F("building__name"),
                building__address_new=F("building__address_new")).values()
        
        for img_report in img_reports:
            img_report["img_urls"] =[]
            if img_report["img_url1"]:
                img_report["img_urls"].append(img_report["img_url1"])
            if img_report["img_url2"]:
                img_report["img_urls"].append(img_report["img_url2"]) 
            if img_report["img_url3"]:
                img_report["img_urls"].append(img_report["img_url3"]) 
            if img_report["img_url4"]:
                img_report["img_urls"].append(img_report["img_url4"]) 
            if img_report["img_url5"]:
                img_report["img_urls"].append(img_report["img_url5"])         
        
        return Response({"data": {"pdf_report":pdf_reports, "img_reports":img_reports}})        



class MonthlyReportListByTime(APIView):
    def get(self, request, year, month):
        user = request.user
        
        if not user.category=="파트너":
            return Response({"Failed":"Invalid user type"})
        
        pdf_reports = models.MonthlyReport.objects.filter(
        year=year,
        month=month,
        partner__user=user.id).values().annotate(
                building__name=F("building__name"),
                building__address_new=F("building__address_new"))
        
        img_reports = models.MonthlyReportImage.objects.filter(
            partner__user=user.id,
            year=year,
            month=month
        ).values().order_by("week").annotate(
                building__name=F("building__name"),
                building__address_new=F("building__address_new"))
        
        return Response({"data": {"pdf_report":pdf_reports, "img_reports":img_reports}})





class MonthlyReportListByTimeAndBuilding(APIView):
    def get(self, request, year, month, building_id, category):
        user = request.user
            
        if not util.validateBuildingId(user, building_id):
            return Response({"Failed": "Invalid Building ID"})
        
        if user.category=="파트너":
            pdf_reports = models.MonthlyReport.objects.filter(
                building=building_id,
                year=year,
                month=month,
                partner__user=user.id,
            ).values().annotate(
                building__name=F("building__name"),
                building__address_new=F("building__address_new")) 
            
            img_reports = models.MonthlyReportImage.objects.filter(
                partner__user=user.id,
                year=year,
                month=month
            ).values().order_by("week").annotate(
                building__name=F("building__name"),
                building__address_new=F("building__address_new")) 
        
        else:    
            pdf_reports = models.MonthlyReport.objects.filter(
                building=building_id,
                year=year,
                month=month,
                partner__category=category,
            ).values().annotate(
                building__name=F("building__name"),
                building__address_new=F("building__address_new"))
            img_reports = models.MonthlyReportImage.objects.filter(
                building=building_id,
                year=year,
                month=month,
                partner__category=category,
            ).values().order_by("week").annotate(
                building__name=F("building__name"),
                building__address_new=F("building__address_new")) 
        
        for img_report in img_reports:
            img_report["img_urls"] =[]
            if img_report["img_url1"]:
                img_report["img_urls"].append(img_report["img_url1"])
            if img_report["img_url2"]:
                img_report["img_urls"].append(img_report["img_url2"]) 
            if img_report["img_url3"]:
                img_report["img_urls"].append(img_report["img_url3"]) 
            if img_report["img_url4"]:
                img_report["img_urls"].append(img_report["img_url4"]) 
            if img_report["img_url5"]:
                img_report["img_urls"].append(img_report["img_url5"])       
        
        return Response({"data": {"pdf_report":pdf_reports, "img_reports":img_reports}})


class MonthlyReportImagePost(APIView):
    @swagger_auto_schema(request_body=serializers.ReportSerializer)
    def post(self, request):
        user = request.user

        if not user.category == "파트너":
            return Response({"Failed": "Invalid user type"})
        
        img_files = request.FILES.getlist("img")
        data = request.POST.get("data")
        data = json.loads(data)
        
        temp = {}
        temp["building"] = Building.objects.get(id=int(data["building"]))
        temp["partner"] = Company.objects.get(user=user)
        temp["year"] = data["year"]
        temp["month"] = data["month"]
        temp["title"] = data["title"]
        temp["week"] = data["week"]
        temp["comment"] = data["comment"]
        for idx, f in enumerate(img_files):
            temp["img_url" + str(idx + 1)] = f.name
            util.upload_to_aws_doc(f)
            
        monthly_report = models.MonthlyReportImage.objects.create(**temp)
        
        tenant_ids = (
            RoomContract.objects.filter(building=temp["building"].id)
            .values_list("tenant__id", flat=True)
            .order_by("tenant__id")
            .distinct()
        )[::1]
        owner_ids = (
            RoomContract.objects.filter(building=temp["building"].id)
            .values_list("owner__id", flat=True)
            .order_by("owner__id")
            .distinct()
        )[::1]
        owner_represent_ids = (
            BuildingContract.objects.filter(building=temp["building"].id)
            .values_list("owner_represent__id", flat=True)
            .order_by("owner_represent__id")
            .distinct()
        )[::1]
        
        recipient_ids = list(set(tenant_ids + owner_ids + owner_represent_ids))

        for recipient_id in recipient_ids:
            recipient = UserAccount(id=recipient_id)

            models.Notification.objects.create(
                recipient=recipient,
                contents=f'{temp["building"].name}빌딩의 월간 보고서가 포스트 되었습니다. 체크해보세요',
                monthly_report_id=monthly_report.id,
                category = "monthly_report",
            )
        return Response({"Success": "Monthly report is successfully uploaded"})

class MonthlyReportPost(APIView):
    @swagger_auto_schema(request_body=serializers.ReportSerializer)
    def post(self, request):
        user = request.user

        if not user.category == "파트너":
            return Response({"Failed": "Invalid user type"})
        
        pdf_file = request.FILES.get("monthly_report")
        data = request.POST.get("data")
        data = json.loads(data)
        temp = {}
        temp["building"] = Building.objects.get(id=int(data["building"]))
        temp["partner"] = Company.objects.get(user=user)
        temp["pdf_url"] = pdf_file.name
        temp["year"] = data["year"]
        temp["month"] = data["month"]
        temp["title"] = data["title"]
        temp["comment"] = data["comment"]
        
        monthly_report = models.MonthlyReport.objects.create(**temp)
        util.upload_to_aws_pdf(pdf_file)
        
        tenant_ids = (
            RoomContract.objects.filter(building=temp["building"].id)
            .values_list("tenant__id", flat=True)
            .order_by("tenant__id")
            .distinct()
        )[::1]
        owner_ids = (
            RoomContract.objects.filter(building=temp["building"].id)
            .values_list("owner__id", flat=True)
            .order_by("owner__id")
            .distinct()
        )[::1]
        owner_represent_ids = (
            BuildingContract.objects.filter(building=temp["building"].id)
            .values_list("owner_represent__id", flat=True)
            .order_by("owner_represent__id")
            .distinct()
        )[::1]
        
        recipient_ids = list(set(tenant_ids + owner_ids + owner_represent_ids))

        for recipient_id in recipient_ids:
            recipient = UserAccount(id=recipient_id)

            models.Notification.objects.create(
                recipient=recipient,
                contents=f'{temp["building"].name}빌딩의 월간 보고서가 포스트 되었습니다. 체크해보세요',
                monthly_report_id=monthly_report.id,
                category = "monthly_report",
            )
        return Response({"Success": "Post is successfully uploaded"})

class MonthlyReportUpdate(APIView):
    def post(self, request, type, id):
        user = request.user
        data = request.POST.get("data")
        data = json.loads(data)
        
        if not user.category == "파트너":
            return Response({"Failed": "Invalid user type"})
        

        if type=="pdf":
            pdf_file = request.FILES.get("monthly_report")
            if pdf_file is not None:
                data["pdf_url"] = pdf_file.name
                util.upload_to_aws_pdf(pdf_file)        

            models.MonthlyReport.objects.filter(id=id).update(**data)
        
        else:
            img_files = request.FILES.getlist("img")
            if img_files is not None:
                for i in range(0,5):
                    data["img_url" + str(i + 1)] =""
                
                for idx, f in enumerate(img_files):
                    data["img_url" + str(idx + 1)] = f.name
                    util.upload_to_aws(f)
            models.MonthlyReportImage.objects.filter(id=id).update(**data)
        return Response({"Success":"Monthly report is successfully updated"})
    
    
class CarSearch(APIView):
    def get(self, request, building_id, car_number):
        user = request.user
        if user.category == "세입자":
            if RoomContract.objects.filter(building=building_id, tenant=user).exists():
                return Response(
                    {
                        "data": RoomContract.objects.filter(
                            building=building_id, car_number=car_number
                        ).values("room_number")
                    }
                )
        elif user.category == "집주인":
            if RoomContract.objects.filter(building=building_id, owner=user).exists():
                return Response(
                    {
                        "data": RoomContract.objects.filter(
                            building=building_id, car_number=car_number
                        ).values("room_number", "tenant__phone_number")
                    }
                )

        elif user.category == "집주인대표":
            if BuildingContract.objects.filter(
                building=building_id, owner_represent=user
            ).exists():
                return Response(
                    {
                        "data": RoomContract.objects.filter(
                            building=building_id, car_number=car_number
                        ).values("room_number", "tenant__phone_number")
                    }
                )
            
        elif user.category == "부동산":
            if BuildingContract.objects.filter(
                building=building_id, agent=user
            ).exists():
                return Response(
                    {
                        "data": RoomContract.objects.filter(
                            building=building_id, car_number=car_number
                        ).values("room_number", "tenant__phone_number")
                    }
                )

        else:
            return Response({"Failed": "No authority on this building"})


class CarSearchList(APIView):
    def get(self, request, building_id):
        user = request.user
        if user.category == "부동산":
            if BuildingContract.objects.filter(
                building=building_id, agent=user
            ).exists():
                return Response(
                    {
                        "data": RoomContract.objects.filter(
                            building=building_id
                        ).values("room_number", "tenant__phone_number", "car_number")
                    }
                )
        elif user.category == "집주인":
            if RoomContract.objects.filter(building=building_id, owner=user).exists():
                return Response(
                    {
                        "data": RoomContract.objects.filter(
                            building=building_id
                        ).values("room_number", "tenant__phone_number", "car_number")
                    }
                )

        elif user.category == "집주인대표":
            if BuildingContract.objects.filter(
                building=building_id, owner_represent=user
            ).exists():
                return Response(
                    {
                        "data": RoomContract.objects.filter(
                            building=building_id
                        ).values("room_number", "tenant__phone_number", "car_number")
                    }
                )

        else:
            return Response({"Failed": "No authority on this building"})


class TenantBillEntireList(APIView):
    def get(self, request):
        user = request.user
        building_ids = util.getBIds4AOR(user)
        bills = (
            models.TenantBill.objects.filter(contract__building__in=building_ids)
            .values(
                "contract__building__id",
                "contract__building__name",
                "contract__tenant__name",
                "paid_date",
            )
            .order_by("contract__building")
        )

        return Response({"data": bills})


class PartnerBillEntireList(APIView):
    def get(self, request):
        user = request.user
        building_ids = util.getBIds4AORT(user)
        bills = models.PartnerBill.objects.filter(
            contract__building__id__in=building_ids
        ).values(
            "contract__building__id",
            "contract__building__name",
            "contract__partner__name",
            "paid_date",
        )
        return Response({"data": bills})


class NoticeList(APIView):
    @swagger_auto_schema(query_serializer=pageSerializer)
    def get(self, request):
        page = request.GET.get("page")
        notices = models.Notice.objects.filter().values()
        paginator = Paginator(notices, 10)
        pages = paginator.get_page(page)
        pages = NoticeSerializer(pages, many=True).data
        return Response({"data": pages, "numpages": paginator.num_pages})


class NotificationList(APIView):
    def get(self, request):
        user = request.user
        notification = models.Notification.objects.filter(
            recipient=int(user.id), is_read=False
        ).order_by("category").values("category").annotate(count=Count("category"))
        return Response({"data":notification})

class NotifiactinoDelete(APIView):
    def delete(self, request, category):
        user = request.user
        models.Notification.objects.filter(
            recipient=int(user.id), category=category
        ).delete()
        return Response({"Success":"Notification is successfully deleted"})

class NotifiactionMarkRead(APIView):
    def get(self, request, notifiaction_id):
        models.Notification.objects.filter(id=notifiaction_id).update(is_read=True)
        return Response({"Success": "Notification is successfully marked as read"})


class RatePartner(APIView):
    def get(self, request, issue_report_id):
        if not models.IssueReport.objects.filter(id=issue_report_id).exists():
            return Response({"Failed":"Invalid issue report id"})
        
        if CompanyRate.objects.filter(issue_report=issue_report_id).exists():
            rate_info = CompanyRate.objects.filter(issue_report=issue_report_id).values()
        else: 
            rate_info = {}
            
        issue_report_info = models.IssueReport.objects.filter(id=issue_report_id).values(
            "issue__id",
            "issue__category", 
            "issue__building__name" ,
            "created_at"
            )[::1][0]
        
        building_id = models.IssueReport.objects.filter(
            id=issue_report_id).values_list("issue__building__id", flat=True)[::1][0]
        
        if not PartnerContract.objects.filter(
            building=building_id, 
            partner__category=issue_report_info["issue__category"]
            ).exists():
            return Response({"Failed":"Something went wrong"})

        
        
        if not PartnerContract.objects.filter(
            building=building_id, 
            partner__category=issue_report_info["issue__category"]
            ).exists():
            return Response({"Failed":"Something went wrong"})
        partner__name = PartnerContract.objects.filter(
            building=building_id, 
            partner__category=issue_report_info["issue__category"]
            ).values_list("partner__name", flat=True)[::1][0]
        issue_report_info["partner__name"]= partner__name
        issue_report_info["rate_info"]= rate_info

        return Response({"data":issue_report_info})

    @swagger_auto_schema(request_body=CompanyRateSerializer)
    def post(self, request, issue_report_id):
        data = self.request.data
        user = request.user
        
        if not models.IssueReport.objects.filter(id=issue_report_id).exists():
            return Response({"Failed":"Invalid issue report id"})
        
        if CompanyRate.objects.filter(issue_report=issue_report_id).exists():
            return Response({"Failed":"Already rated"})
        
        issue_report = models.IssueReport(id=issue_report_id)
        infos = models.IssueReport.objects.filter(
            id=issue_report_id).values("issue__building__id","issue__id")[::1][0]
        issue_id =  infos["issue__id"]
        building_id = infos["issue__building__id"]
        category = models.Issue.objects.filter(id=issue_id
                                            ).values_list("category", flat=True)[::1][0]
        company_id = PartnerContract.objects.filter(
            partner__category=category, building_id=building_id
            ).values_list("partner__id", flat=True)[::1][0]
        company = Company(id=company_id)
        data["issue_report"]=issue_report
        data["company"]=company
        data["user"]=user
        CompanyRate.objects.create(**data)
        return Response({"Success":"Comapny is successfuly rated"})

class MyRatePartner(APIView):
    def get(self, request):
        user = request.user
        rates = CompanyRate.objects.filter(user=user).values(
            "id",
            "total_rate",
            "satisfaction_rate",
            "kindness_rate",
            "waiting_time_rate",
            "etc",
            "company__id",
            "company__name",
            "issue_report__issue__request_date", 
            "issue_report__issue__category", 
            "issue_report__issue__building__name", 
            "issue_report__issue__room_number",
            )
        return Response({"data":rates})

@swagger_auto_schema(methods=["get"], tags=["차량조회"])
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def CarListRead(request, building_id, car_number):
    user = request.user
    if user.category == "관리자" or user.category == "동대표" or user.category == "입주민":
        carnumber_list = RoomContract.objects.filter(
            building_id=building_id, car_number__contains=car_number
            ).values("tenant__phone_number","room_number","car_number")
            
        return Response(carnumber_list)
    else : 
        return Response([])

@swagger_auto_schema(methods=["post"], tags=["하자보수"])
@api_view(["POST"])
@permission_classes([IsAuthenticated])
def RepairReportCreate( request, building_id, repair_report_id ):
    user = request.user
    data = request.data
    if not user.category == "관리자":
        return Response({"Failed": "Invalid user type"})

    img_files = request.FILES.getlist('img')

    if not img_files:
        return Response({"Failed": "No file uploaded"})

    result_obj = {}
    result_obj["title"] = data["title"]
    result_obj["content"] = data["content"]
    result_obj["building_id"] = building_id
    result_obj["agent_id"] = user.id

    for idx, img_file in enumerate(img_files):
            img_file.name = generate_filename(img_file)
            result_obj["img_url" + str(idx + 1)] = img_file.name
            util.upload_to_aws(img_file)

    if repair_report_id == 0:
        repair_report = RepairReport.objects.create(**result_obj)
        return Response({"Created"})
    else : 
        repair_report = RepairReport.objects.filter(id=repair_report_id).update(**result_obj)
        return Response({"Updated"})
    

@swagger_auto_schema(methods=["get"], tags=["하자보수"])
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def RepairReportRead(request, building_id):
    repair_report_info = RepairReport.objects.filter(building_id = building_id).values().order_by('-createdAt')
    return Response(repair_report_info)

@swagger_auto_schema(methods=["delete"], tags=["하자보수"])
@api_view(["DELETE"])
@permission_classes([IsAuthenticated])
def RepairReportDelete(request, building_id, repair_report_id):
    user = request.user
    if BuildingContract.objects.filter(id = building_id, agent_id = user.id).exists() :
        if RepairReport.objects.filter(id = repair_report_id).exists() :
            RepairReport.objects.filter(id = repair_report_id).delete()
            return Response({"Success": "Deleted"})
        else : 
            return Response({"Failed": "Invalid repair_report_id"})
    else :
        return Response({"Failed": "Invalid user type"})


@swagger_auto_schema(methods=["post"], tags=["월간보고서"])
@api_view(["POST"])
@permission_classes([IsAuthenticated])
def MonthlyReportCreate(request, building_id, monthly_report_id, year, month):
    user = request.user
    data = request.data
    if not user.category == "관리자":
        return Response({"Failed": "Invalid user type"})

    doc_files = request.FILES.getlist('doc')

    if not doc_files:
        return Response({"Failed": "No file uploaded"})

    result_obj = {}
    result_obj["title"] = data["title"]
    result_obj["content"] = data["content"]
    result_obj["building_id"] = building_id
    result_obj["agent_id"] = user.id
    result_obj["year"] = year
    result_obj["month"] = month

    for idx, doc_file in enumerate(doc_files):
            doc_file.name = generate_filename(doc_file)
            result_obj["doc_url" + str(idx + 1)] = doc_file.name
            util.upload_to_aws_doc(doc_file)

    if monthly_report_id == 0:
        monthly_report = MonthlyReport.objects.create(**result_obj)
        return Response({"Created"})
    else : 
        monthly_report = MonthlyReport.objects.filter(id=monthly_report_id).update(**result_obj)
        return Response({"Updated"})
    

@swagger_auto_schema(methods=["get"], tags=["월간보고서"])
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def MonthlyReportRead(request, building_id, year):
    monthly_report_info = MonthlyReport.objects.filter(building_id = building_id, year = year).values().order_by('month')
    return Response(monthly_report_info)

@swagger_auto_schema(methods=["delete"], tags=["월간보고서"])
@api_view(["DELETE"])
@permission_classes([IsAuthenticated])
def MonthlyReportDelete(request, building_id, monthly_report_id):
    user = request.user
    if BuildingContract.objects.filter(id = building_id, agent_id = user.id).exists() :
        if MonthlyReport.objects.filter(id = monthly_report_id).exists() :
            MonthlyReport.objects.filter(id = monthly_report_id).delete()
            return Response({"Success": "Deleted"})
        else : 
            return Response({"Failed": "Invalid monthly_report_id"})
    else :
        return Response({"Failed": "Invalid user type"})
    
@swagger_auto_schema(methods=["get"], tags=["관리비"])
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def TenantBillReadList(request, building_id):
    user = request.user
    print(user.category,"user.category")
    
    result_arr = []
    if user.category == "관리자":
        room_infos = RoomContract.objects.filter(building_id = building_id).values('id','room_number')
        for room_info in room_infos :
            non_paid_amount = 0
            bill_infos = TenantBill.objects.filter(contract_id = room_info['id'], is_paid = 0).values()
            if bill_infos :
                for bill_info in bill_infos :
                    non_paid_amount += bill_info['monthly_charge']
            result_obj = {}
            result_obj['id'] = room_info['id']
            result_obj['room_number'] = room_info['room_number']
            result_obj['non_paid_amount'] = non_paid_amount
            result_arr.append(result_obj)
    elif user.category == "동대표":
        room_infos = RoomContract.objects.filter(building_id = building_id).values('id','room_number')
        for room_info in room_infos :
            non_paid_amount = 0
            bill_infos = TenantBill.objects.filter(contract_id = room_info['id'], is_paid = 0).values()
            non_paid_amount += len(bill_infos)
            result_obj = {}
            result_obj['id'] = room_info['id']
            result_obj['room_number'] = room_info['room_number']
            result_obj['non_paid_amount'] = non_paid_amount
            result_arr.append(result_obj)
    return Response(result_arr)

@swagger_auto_schema(methods=["get"], tags=["관리비"])
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def TenantBillReadByMonthOne(request, year, month):
    user = request.user
    if user.category == "동대표" or user.category == "입주민" :
        room_info = RoomContract.objects.filter(tenant_id = user.id).values('id','monthly_charge','paying_date','contract_start','contract_end')[0]
        print(room_info['id'], "room_info['id']")
        if TenantBill.objects.filter(contract_id = room_info['id'], year = year, month = month).exists() :
            bill_info = TenantBill.objects.filter(contract_id = room_info['id'], year = year, month = month).values('id','monthly_charge','is_paid')[0]
            bill_info['paying_date'] = room_info['paying_date']
            return Response(bill_info)
        else :
            bill_info = {}
            bill_info['monthly_charge'] = room_info['monthly_charge']
            bill_info['is_paid'] = False
            bill_info['paying_date'] = room_info['paying_date']
            return Response(bill_info)
    else : 
        return Response({"Message":"Wrong access"})

@swagger_auto_schema(methods=["get"], tags=["관리비"])
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def TenantBillReadByRoom(request, year, room_id):
    user = request.user
    print(user.category,"user.category")
    result_arr = []
    
    if user.category == "관리자":
        if not RoomContract.objects.filter(id = room_id).exists() :
            return Response([])
        room_info = RoomContract.objects.filter(id = room_id).values()[0]
        if not TenantBill.objects.filter(year = year, contract_id = room_id).exists() :
            return Response([])
        bill_infos = TenantBill.objects.filter(year = year, contract_id = room_id).values().order_by('month')
        total_non_paid_amount = 0
        for bill_info in bill_infos : 
            result_obj = {}
            result_obj['month'] = bill_info['month']
            result_obj['paid_date'] = bill_info['paid_date']
            result_obj['is_paid'] = bill_info['is_paid']
            result_obj['monthly_charge'] = bill_info['monthly_charge']
            if not bill_info['is_paid'] :
                result_obj['non_paid_amount'] = bill_info['monthly_charge']
            else :
                result_obj['non_paid_amount'] = 0
            total_non_paid_amount += result_obj['non_paid_amount']
            result_arr.append(result_obj)
        result_arr[0]['total_non_paid_amount'] = total_non_paid_amount
        result_arr[0]['current_monthly_charge'] = room_info['monthly_charge']
        return Response(result_arr)

    elif user.category == "동대표":
        if not TenantBill.objects.filter(year = year, contract_id = room_id).exists() :
            return Response([])
        bill_infos = TenantBill.objects.filter(year = year, contract_id = room_id).values().order_by('month')
        total_non_paid_amount = 0
        for bill_info in bill_infos : 
            result_obj = {}
            result_obj['month'] = bill_info['month']
            result_obj['paid_date'] = bill_info['paid_date']
            result_obj['is_paid'] = bill_info['is_paid']
            result_arr.append(result_obj)
        return Response(result_arr)

    return Response([])
    

@swagger_auto_schema(methods=["get"], tags=["관리비"])
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def TenantBillReadNotPaid(request, building_id):
    user = request.user
    user_info = UserAccount.objects.filter(id = user.id).values()[0]
    if user.category == "동대표" or user.category == "입주민" :
        room_info = RoomContract.objects.filter(tenant_id = user.id, building_id = building_id).values()[0]
        not_paid_infos = TenantBill.objects.filter(contract_id=room_info['id'], is_paid = 0).values()
        for not_paid_info in not_paid_infos :
            not_paid_info['customerKey'] = user.id 
            not_paid_info['orderId'] = generate_random_filename( str(room_info['id']) + "_" +str(not_paid_info['year']) + "_" + str(not_paid_info['month'])) 
            not_paid_info['orderName'] = str(not_paid_info['year']) + "년 " + str(not_paid_info['month']) + "월 관리비 결제"
            not_paid_info['customerName'] = str(user_info['name'])

        return Response(not_paid_infos)

@swagger_auto_schema(methods=["post"], tags=["관리비"])
@api_view(["POST"])
def payment_confirm(request):
    secret_key = "test_sk_Wd46qopOB89JJmym1aK3ZmM75y0v"
    auth = f"{secret_key}:"
    auth_header = base64.b64encode(auth.encode('utf-8')).decode('utf-8')
    payload = json.dumps(request.data).encode('utf-8')
    conn = http.client.HTTPSConnection("api.tosspayments.com")
    print(auth_header,"auth_header")
    headers = {
        'Authorization': f'Basic {auth_header}',
        'Content-Type': "application/json"
    }
    payload_dict = json.loads(payload)
    temp_str = payload_dict['orderId'].split("_")
    room_id = temp_str[1]
    year = temp_str[2]
    month = temp_str[3]
    conn.request("POST", "/v1/payments/confirm", payload, headers)
    res = conn.getresponse()
    data = res.read().decode("utf-8")
    
    data_dict = json.loads(data)
    print(data,"data")
    
    BillLog.objects.create(
        room_id=temp_str[1],
        year=temp_str[2],
        month=temp_str[3],
        amount=payload_dict['amount'],
        issued_date=datetime.datetime.now(),
        bill_method=""
    )
    TenantBill.objects.filter(contract_id=room_id, year=year, month=month).update(is_paid=True)

    return JsonResponse({'message': 'success', 'data': data})  # 처리 결과를 JsonResponse로 반환

@swagger_auto_schema(methods=["post"], tags=["결제"])
@api_view(["POST"])
def PaymentSuccess(request):
    serializer = PaymentSuccessSerializer(data=request.data)
    if serializer.is_valid():
        # Payment success logic here
        return Response({'success': True})
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

@swagger_auto_schema(methods=["post"], tags=["결제"])
@api_view(["POST"])
def PaymentFail(request):
    serializer = PaymentCancelSerializer(data=request.data)
    if serializer.is_valid():
        # Payment cancel logic here
        return Response({'success': True})
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)