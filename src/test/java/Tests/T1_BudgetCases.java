package Tests;

import Backoffice_Pages.P1_LoginPage;
import Backoffice_Pages.P4_OrgStructurePage;
import Backoffice_Pages.P6_HomePage;
import DriverFactory.DriverFactoryClass;
import Listeners.IInvociedMethodListener;
import Listeners.ITestResultListener;
import Listeners.ResultReportListener;
import Utilities.DataUtiles;
import Utilities.JsonUtil;
import Utilities.LogsUtiles;
import Utilities.Utility;
import Utilities.Validations;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;

import static DriverFactory.DriverFactoryClass.SetupDriver;
import static DriverFactory.DriverFactoryClass.getdriver;

    // دورة الميزانية كلها بالترتيب: كل كيس بيقرا اللي قبله من runtime.json
    // اللوجين ← وحدة تقويم ← الهيكل التنظيمي ← طلب ميزانية واعتماداته ← طلب تعزيز واعتماداته
    @Listeners({IInvociedMethodListener.class, ITestResultListener.class, ResultReportListener.class})
    public class T1_BudgetCases {
        private WebDriver driver ;

        // براوزر واحد للدورة كلها وبنعمل لوج اوت بعد كل كيس بدل ما نقفله
        // (الملفات بتتحمل مرة واحدة وبعدها من الكاش، والرن نزل من ~29 دقيقة لـ ~14)
        // البراوزر واللينك من environment.properties فأي تغيير هناك بيتطبق هنا على طول
        @BeforeMethod(alwaysRun = true)
        public void setupdriver () throws IOException {
            if (getdriver() == null) SetupDriver(DataUtiles.getPropertyValue("environment","BROWSER"));
            driver = DriverFactoryClass.getdriver();
            getdriver().get(DataUtiles.getPropertyValue("environment", "Backoffice_URL"));
            Utility.WaitForPageLoad(driver);
        }

        // بيدخل باليوزر ويروح للرئيسية ويفتح مهمة آخر طلب (الميزانية أو التعزيز) من مهامي
        private P6_HomePage OpenLastTaskAs (String User , String RequestNumberKey) throws InterruptedException {
            return new P1_LoginPage(driver).Login_As_Admin(DataUtiles.getJsonData("Data",User + "Email")
                            ,DataUtiles.getJsonData("Data",User + "Password"))
                    .Open_EServices().Select_HomePage()
                    .OpenTask(JsonUtil.readValue(RequestNumberKey));
        }


        // ===== اللوجين =====
        @Test(priority = 1, description = "تسجيل دخول المسؤول")
        public void Login_As_Admin () throws InterruptedException {
            String UserName = new P1_LoginPage(driver).Login_As_Admin(DataUtiles.getJsonData("Data","AdminUser")
                    ,DataUtiles.getJsonData("Data","AdminPassword")).GetLoggedInUserName();

            // اسم اليوزر في الهيدر = اللوجين نجح ودخلنا بنفس اليوزر
            Validations.ValidateContains(UserName, DataUtiles.getJsonData("Data","AdminName"));
        }


        // ===== التقويم الوطني =====
        @Test(priority = 2, description = "إضافة وحدة تقويم إلى السنة")
        public void AddCalendarUnitToYear () throws InterruptedException {
            // الاسم كل رن مختلف: وحدة اوتوميشن + اليوم والشهر والساعة والدقيقة
            String NameAr = DataUtiles.getJsonDataWithDateAndTime("Data","CalendarUnitName");
            String NameEn = DataUtiles.getJsonDataWithDateAndTime("Data","CalendarUnitNameEn");

            boolean Displayed = new P1_LoginPage(driver).Login_As_Admin(DataUtiles.getJsonData("Data","AdminUser")
                            ,DataUtiles.getJsonData("Data","AdminPassword"))
                    .Open_EServices().Select_ManageEventsCalendarPage()
                    .AddCalendarUnitToYear(DataUtiles.getJsonData("Data","CalendarYear"), NameAr, NameEn
                            ,DataUtiles.getJsonData("Data","CalendarUnitType"))
                    .IsCalendarUnitDisplayed(NameAr);

            Validations.ValidationTrue(Displayed, "Calendar unit " + NameAr + " is not displayed after saving");
            // آخر وحدة اتعملت عشان أي كيس بعد كده يشتغل عليها
            JsonUtil.writeValue("LastCalendarUnitName", NameAr);
        }


        // ===== الهيكل التنظيمي =====
        // بيضيف اليوزرز بالترتيب تحت آخر وحدة تقويم اتعملت، وبعدهم قائد الموسم
        @Test(priority = 3, description = "إضافة المستخدمين إلى الهيكل التنظيمي لآخر وحدة")
        public void AddUsersToOrgStructureOfLastUnit () throws InterruptedException {
            String[] Users = {"SeasonOffice", "UnitManager", "BudgetEmployee", "BudgetManager"};

            P4_OrgStructurePage OrgStructure = new P1_LoginPage(driver).Login_As_Admin(DataUtiles.getJsonData("Data","AdminUser")
                            ,DataUtiles.getJsonData("Data","AdminPassword"))
                    .Open_EServices().Select_OrgStructurePage()
                    .FilterByCalendarUnit(DataUtiles.getJsonData("Data","CalendarYear"), JsonUtil.readValue("LastCalendarUnitName"));

            for (String User : Users) {
                OrgStructure.AddUserToUnit(DataUtiles.getJsonData("Data",User + "NameAr"), DataUtiles.getJsonData("Data",User + "NameEn")
                        ,DataUtiles.getJsonData("Data",User + "Role"), DataUtiles.getJsonData("Data",User + "UserName"));
            }
            // قائد الموسم بيعتمد المناقلة بعد مدير الموسم، ومن غير صلاحيات اعتماد لأن مدير الوحدة خدها كلها
            // (كل صلاحية بتتمنح لدور واحد بس في الموسم)
            OrgStructure.AddUserToUnit(DataUtiles.getJsonData("Data","SeasonLeaderNameAr"), DataUtiles.getJsonData("Data","SeasonLeaderNameEn")
                    ,DataUtiles.getJsonData("Data","SeasonLeaderRole"), DataUtiles.getJsonData("Data","SeasonLeaderUserName"), false);
            OrgStructure.SaveOrgStructure();

            Validations.ValidationTrue(OrgStructure.IsNodeDisplayed(DataUtiles.getJsonData("Data","SeasonLeaderNameAr"))
                    , "SeasonLeader is not displayed in the org structure");
            for (String User : Users)
                Validations.ValidationTrue(OrgStructure.IsNodeDisplayed(DataUtiles.getJsonData("Data",User + "NameAr"))
                        , User + " is not displayed in the org structure");
        }


        // ===== طلب الميزانية =====
        // مكتب إدارة الموسم بيعمل طلب ميزانية لآخر وحدة، يحفظه مسودة ويرسله على طول من نفس الصفحة
        @Test(priority = 4, description = "إنشاء طلب ميزانية لآخر وحدة وإرساله")
        public void CreateAndSendBudgetRequestOfLastUnit () throws InterruptedException {
            String AttachmentPath = System.getProperty("user.dir") + "/src/test/TestData/" + DataUtiles.getJsonData("Data","AttachmentFile");

            boolean Sent = new P1_LoginPage(driver).Login_As_Admin(DataUtiles.getJsonData("Data","SeasonOfficeEmail")
                            ,DataUtiles.getJsonData("Data","SeasonOfficePassword"))
                    .Open_EServices().Select_BudgetsPage()
                    .FilterByCalendarUnit(DataUtiles.getJsonData("Data","CalendarYear"), JsonUtil.readValue("LastCalendarUnitName"))
                    .CreateBudgetRequest(Long.parseLong(DataUtiles.getJsonData("Data","BudgetTotal"))
                            ,DataUtiles.getJsonData("Data","BudgetNewItemNameAr"), DataUtiles.getJsonData("Data","BudgetNewItemNameEn")
                            ,DataUtiles.getJsonData("Data","BudgetDetails"), AttachmentPath)
                    .SaveAsDraft()
                    .SendRequest();

            Validations.ValidationTrue(Sent, "Budget request " + JsonUtil.readValue("LastBudgetRequestNumber") + " was not sent");
        }


        // ===== اعتمادات طلب الميزانية: مدير الوحدة ← موظف الميزانية ← مدير الميزانية ← المدير العام =====
        @Test(priority = 5, description = "اعتماد مدير الوحدة لطلب الميزانية")
        public void UnitManagerApprovesLastBudgetRequest () throws InterruptedException {
            boolean Approved = OpenLastTaskAs("UnitManager", "LastBudgetRequestNumber").ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Unit manager couldn't approve the budget request");
        }

        // موظف الميزانية: يعيّن الطلب لنفسه، يفتحه تاني، يكمل بيانات البنود ويرسله للاعتماد
        @Test(priority = 6, description = "إرسال مختص الميزانية الطلب للاعتماد")
        public void BudgetEmployeeSendsLastBudgetRequestForApproval () throws InterruptedException {
            OpenLastTaskAs("BudgetEmployee", "LastBudgetRequestNumber").AssignTaskToMe();
            boolean Sent = new P6_HomePage(driver).OpenTask(JsonUtil.readValue("LastBudgetRequestNumber"))
                    .CompleteBudgetItemsData()
                    .ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Sent, "Budget employee couldn't send the budget request for approval");
        }

        @Test(priority = 7, description = "اعتماد مدير الميزانية لطلب الميزانية")
        public void BudgetManagerApprovesLastBudgetRequest () throws InterruptedException {
            boolean Approved = OpenLastTaskAs("BudgetManager", "LastBudgetRequestNumber").ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Budget manager couldn't approve the budget request");
        }

        @Test(priority = 8, description = "اعتماد المدير العام لطلب الميزانية")
        public void BudgetGeneralManagerApprovesLastBudgetRequest () throws InterruptedException {
            boolean Approved = OpenLastTaskAs("BudgetGeneralManager", "LastBudgetRequestNumber").ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Budget general manager couldn't approve the budget request");
        }


        // ===== طلب التعزيز =====
        // مكتب إدارة الموسم بيعزز البند اللي ضافه بإيده في ميزانية آخر وحدة (لازم تكون اتعتمدت قبله)
        @Test(priority = 9, description = "إنشاء طلب تعزيز لبند الاختبار وإرساله")
        public void CreateAndSendSupplementRequestOfLastUnit () throws InterruptedException {
            String AttachmentPath = System.getProperty("user.dir") + "/src/test/TestData/" + DataUtiles.getJsonData("Data","AttachmentFile");

            boolean Sent = new P1_LoginPage(driver).Login_As_Admin(DataUtiles.getJsonData("Data","SeasonOfficeEmail")
                            ,DataUtiles.getJsonData("Data","SeasonOfficePassword"))
                    .Open_EServices().Select_SupplementRequestsPage()
                    .FilterByCalendarUnit(DataUtiles.getJsonData("Data","CalendarYear"), JsonUtil.readValue("LastCalendarUnitName"))
                    .CreateSupplementRequest(Long.parseLong(DataUtiles.getJsonData("Data","SupplementAmount"))
                            ,DataUtiles.getJsonData("Data","BudgetNewItemNameAr")
                            ,DataUtiles.getJsonData("Data","SupplementJustification"), AttachmentPath)
                    .SaveAsDraft()
                    .SendRequest();

            Validations.ValidationTrue(Sent, "Supplement request " + JsonUtil.readValue("LastSupplementRequestNumber") + " was not sent");
        }


        // ===== اعتمادات طلب التعزيز: نفس الترتيب، وموظف الميزانية بيعيّن الطلب لنفسه ويعتمد (بند سابق فمفيش بيانات بنود) =====
        @Test(priority = 10, description = "اعتماد مدير الوحدة لطلب التعزيز")
        public void UnitManagerApprovesLastSupplementRequest () throws InterruptedException {
            boolean Approved = OpenLastTaskAs("UnitManager", "LastSupplementRequestNumber").ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Unit manager couldn't approve the supplement request");
        }

        @Test(priority = 11, description = "اعتماد مختص الميزانية لطلب التعزيز")
        public void BudgetEmployeeApprovesLastSupplementRequest () throws InterruptedException {
            OpenLastTaskAs("BudgetEmployee", "LastSupplementRequestNumber").AssignTaskToMe();
            boolean Approved = new P6_HomePage(driver).OpenTask(JsonUtil.readValue("LastSupplementRequestNumber"))
                    .ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Budget employee couldn't approve the supplement request");
        }

        @Test(priority = 12, description = "اعتماد مدير الميزانية لطلب التعزيز")
        public void BudgetManagerApprovesLastSupplementRequest () throws InterruptedException {
            boolean Approved = OpenLastTaskAs("BudgetManager", "LastSupplementRequestNumber").ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Budget manager couldn't approve the supplement request");
        }

        @Test(priority = 13, description = "اعتماد المدير العام لطلب التعزيز")
        public void BudgetGeneralManagerApprovesLastSupplementRequest () throws InterruptedException {
            boolean Approved = OpenLastTaskAs("BudgetGeneralManager", "LastSupplementRequestNumber").ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Budget general manager couldn't approve the supplement request");
        }


        // ===== طلب المناقلة =====
        // مكتب إدارة الموسم بينقل مبلغ من البند اللي ضافه بإيده لأول بند في القايمة (بعد التعزيز، والميزانية لازم تكون اتعتمدت قبله)
        @Test(priority = 14, description = "إنشاء طلب مناقلة من بند الاختبار وإرساله")
        public void CreateAndSendTransferRequestOfLastUnit () throws InterruptedException {
            String AttachmentPath = System.getProperty("user.dir") + "/src/test/TestData/" + DataUtiles.getJsonData("Data","AttachmentFile");

            boolean Sent = new P1_LoginPage(driver).Login_As_Admin(DataUtiles.getJsonData("Data","SeasonOfficeEmail")
                            ,DataUtiles.getJsonData("Data","SeasonOfficePassword"))
                    .Open_EServices().Select_TransferRequestsPage()
                    .FilterByCalendarUnit(DataUtiles.getJsonData("Data","CalendarYear"), JsonUtil.readValue("LastCalendarUnitName"))
                    .CreateTransferRequest(DataUtiles.getJsonData("Data","BudgetNewItemNameAr")
                            ,Long.parseLong(DataUtiles.getJsonData("Data","TransferAmount"))
                            ,DataUtiles.getJsonData("Data","TransferJustification"), AttachmentPath)
                    .SaveAsDraft()
                    .SendRequest();

            Validations.ValidationTrue(Sent, "Transfer request " + JsonUtil.readValue("LastTransferRequestNumber") + " was not sent");
        }


        // ===== اعتمادات طلب المناقلة: مدير الموسم ← قائد الموسم ← مختص الميزانية ← مدير الميزانية ← مدير عام الميزانية ← الرئيس التنفيذي للمالية ← مدير عام المركز =====
        // مدير وحدة التقويم هو اللي بيعمل دور مدير الموسم في المناقلة
        @Test(priority = 15, description = "اعتماد مدير الموسم لطلب المناقلة")
        public void UnitManagerApprovesLastTransferRequest () throws InterruptedException {
            boolean Approved = OpenLastTaskAs("UnitManager", "LastTransferRequestNumber").ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Unit manager couldn't approve the transfer request");
        }

        // المهمة بتروحله عشان قائد الموسم موجود في الهيكل التنظيمي (لو مش موجود بتعدي على مختص الميزانية على طول)
        @Test(priority = 16, description = "اعتماد قائد الموسم لطلب المناقلة")
        public void SeasonLeaderApprovesLastTransferRequest () throws InterruptedException {
            boolean Approved = OpenLastTaskAs("SeasonLeader", "LastTransferRequestNumber").ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Season leader couldn't approve the transfer request");
        }

        // مختص الميزانية: يعيّن الطلب لنفسه ويفتحه تاني ويعتمد
        @Test(priority = 17, description = "اعتماد مختص الميزانية لطلب المناقلة")
        public void BudgetEmployeeApprovesLastTransferRequest () throws InterruptedException {
            OpenLastTaskAs("BudgetEmployee", "LastTransferRequestNumber").AssignTaskToMe();
            boolean Approved = new P6_HomePage(driver).OpenTask(JsonUtil.readValue("LastTransferRequestNumber"))
                    .ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Budget employee couldn't approve the transfer request");
        }

        @Test(priority = 18, description = "اعتماد مدير الميزانية لطلب المناقلة")
        public void BudgetManagerApprovesLastTransferRequest () throws InterruptedException {
            boolean Approved = OpenLastTaskAs("BudgetManager", "LastTransferRequestNumber").ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Budget manager couldn't approve the transfer request");
        }

        @Test(priority = 19, description = "اعتماد مدير عام الميزانية لطلب المناقلة")
        public void BudgetGeneralManagerApprovesLastTransferRequest () throws InterruptedException {
            boolean Approved = OpenLastTaskAs("BudgetGeneralManager", "LastTransferRequestNumber").ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Budget general manager couldn't approve the transfer request");
        }

        @Test(priority = 20, description = "اعتماد الرئيس التنفيذي للمالية لطلب المناقلة")
        public void CFOApprovesLastTransferRequest () throws InterruptedException {
            boolean Approved = OpenLastTaskAs("CFO", "LastTransferRequestNumber").ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "CFO couldn't approve the transfer request");
        }

        // آخر اعتماد: بعده المناقلة بتتنفذ على البنود
        @Test(priority = 21, description = "اعتماد مدير عام المركز لطلب المناقلة")
        public void CenterManagerApprovesLastTransferRequest () throws InterruptedException {
            boolean Approved = OpenLastTaskAs("CenterManager", "LastTransferRequestNumber").ApproveTask(DataUtiles.getJsonData("Data","ApprovalNotes"));
            Validations.ValidationTrue(Approved, "Center manager couldn't approve the transfer request");
        }


        // بعد كل كيس بنخرج من التطبيق والبوابة عشان الكيس الجاي يدخل بيوزره
        // ولو الخروج فشل (الكيس وقع والصفحة واقفة) بنقفل البراوزر والكيس الجاي يفتح واحد نضيف
        // alwaysRun عشان ده يحصل حتى لو الكيس فشل
        @AfterMethod(alwaysRun = true)
        public void logout () {
            try {
                new P1_LoginPage(driver).Logout();
            } catch (Exception e) {
                LogsUtiles.info("Logout failed, closing the browser: " + e.getMessage());
                DriverFactoryClass.Quit();
            }
        }

        @AfterClass(alwaysRun = true)
        public void quit () {
            DriverFactoryClass.Quit();
        }
}
