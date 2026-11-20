package Backoffice_Pages;

import Utilities.LogsUtiles;
import Utilities.Utility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class P2_Dashboard {
    public P2_Dashboard (WebDriver driver){
        this.driver = driver ;
    }
    private WebDriver driver;

    // زرار اسم اليوزر في الهيدر مش بيظهر غير بعد اللوجين، فبنستخدمه نتأكد إن اللوجين نجح
    private final By UserNameButton = By.id("userActions");
    // "ابدأ الآن" في كارت الخدمات الإلكترونية، بالجزء الثابت من اللينك عشان دومين البيئة (t- / stg-) بيتغير
    private final By EServicesStartNowButton = By.xpath("//a[contains(@href,'/#/dashboard')]");
    // التقويم الوطني ملوش حاجة يونيك، فبنمسكه بإنه القايمة اللي جواها لينك إدارة تقويم الفعاليات
    private final By NationalCalendar_FSM = By.xpath("//li[contains(@class,'sub-links')][.//a[@href='#/national-calendar/managecalenders']]/a");
    private final By ManageEventsCalendarPageButton = By.xpath("//a[@href='#/national-calendar/managecalenders']");
    // إدارة المواسم والفعاليات نفس الفكرة: القايمة اللي جواها لينك الهيكل التنظيمي
    private final By SeasonsAndEventsManagement_FSM = By.xpath("//li[contains(@class,'sub-links')][.//a[@href='/#/ems/orgstructure']]/a");
    private final By OrgStructurePageButton = By.xpath("//a[@href='/#/ems/orgstructure']");
    // الميزانية والتخطيط: القايمة اللي جواها لينك الميزانيات
    private final By BudgetAndPlanning_FSM = By.xpath("//li[contains(@class,'sub-links')][.//a[@href='/#/v2/budget/event-budget-requests/budget-request']]/a");
    private final By BudgetsPageButton = By.xpath("//a[@href='/#/v2/budget/event-budget-requests/budget-request']");
    // طلبات التعزيز في نفس قايمة الميزانية والتخطيط
    private final By SupplementRequestsPageButton = By.xpath("//a[@href='/#/v2/budget/supplement-requests/list']");
    // وطلبات المناقلة كمان
    private final By TransferRequestsPageButton = By.xpath("//a[@href='/#/v2/budget/transfer-requests/list']");


    public String GetLoggedInUserName () {
        String UserName = new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.visibilityOfElementLocated(UserNameButton)).getText().trim();
        LogsUtiles.info("Logged in user is " + UserName);
        return UserName;
    }

    // بيفتح الخدمات الإلكترونية وبيرجع للداش بورد عشان منها نختار أي شاشة
    public P2_Dashboard Open_EServices () {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,EServicesStartNowButton);
        return this;
    }

    public P3_ManageEventsCalendarPage Select_ManageEventsCalendarPage () {
        Utility.OPEN_MENU_AND_CLICK(driver,NationalCalendar_FSM,ManageEventsCalendarPageButton);
        return new P3_ManageEventsCalendarPage(driver);
    }

    public P4_OrgStructurePage Select_OrgStructurePage () {
        Utility.OPEN_MENU_AND_CLICK(driver,SeasonsAndEventsManagement_FSM,OrgStructurePageButton);
        return new P4_OrgStructurePage(driver);
    }

    public P5_BudgetsPage Select_BudgetsPage () {
        Utility.OPEN_MENU_AND_CLICK(driver,BudgetAndPlanning_FSM,BudgetsPageButton);
        return new P5_BudgetsPage(driver);
    }

    public P7_SupplementRequestsPage Select_SupplementRequestsPage () {
        Utility.OPEN_MENU_AND_CLICK(driver,BudgetAndPlanning_FSM,SupplementRequestsPageButton);
        return new P7_SupplementRequestsPage(driver);
    }

    public P8_TransferRequestsPage Select_TransferRequestsPage () {
        Utility.OPEN_MENU_AND_CLICK(driver,BudgetAndPlanning_FSM,TransferRequestsPageButton);
        return new P8_TransferRequestsPage(driver);
    }

    // الخدمات الإلكترونية بتفتح على الرئيسية أصلاً، فالصفحة دي هي اللي فيها مهامي
    public P6_HomePage Select_HomePage () {
        return new P6_HomePage(driver);
    }
}
