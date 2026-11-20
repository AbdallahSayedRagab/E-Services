package Backoffice_Pages;

import Utilities.LogsUtiles;
import Utilities.Scrolling;
import Utilities.Utility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class P3_ManageEventsCalendarPage {
    public P3_ManageEventsCalendarPage (WebDriver driver){
        this.driver = driver ;
    }
    private WebDriver driver;

    // قايمة التلت نقط بتتفتح في آخر الصفحة (container=body) مش جوه الكارت، و"اضافة وحدة تقويم" هي الوحيدة اللي فيها queryparamshandling
    private final By AddCalendarUnitButton = By.xpath("//ul[@role='menu']//button[@queryparamshandling]");

    private final By CalendarUnitNameAr = By.id("eventname");
    private final By CalendarUnitNameEn = By.id("calendernameen");
    private final By MainCategoryDropdown = By.xpath("(//p-dialog//label[contains(@class,'required')]/following-sibling::p-dropdown)[1]");
    private final By SubCategoryDropdown = By.xpath("(//p-dialog//label[contains(@class,'required')]/following-sibling::p-dropdown)[2]");
    private final By FirstDropdownOption = By.xpath("(//li[@role='option'])[1]");
    private final By StartDateField = By.id("startDateCalendar");
    private final By EndDateField = By.id("endDateCalendar");
    private final By TodayInCalendar = By.xpath("//div[contains(@class,'p-datepicker')]//td[contains(@class,'p-datepicker-today')]/span");
    private final By LastEnabledDayInCalendar = By.xpath("(//div[contains(@class,'p-datepicker')]//td[not(contains(@class,'p-datepicker-other-month'))]/span[not(contains(@class,'p-disabled'))])[last()]");
    private final By SaveButton = By.xpath("//p-dialog//button[contains(@class,'btn-black')]");

    // زرار التلت نقط في كارت السنة المطلوبة
    private By YearActionsButton (String Year) {
        return By.xpath("//div[contains(@class,'card-title')]/span[normalize-space()='" + Year + "']/ancestor::div[contains(@class,'card-box')]//button[@dropdowntoggle]");
    }

    // نوع الوحدة (موسم / فعالية / مهرجان ...): الـ radio نفسه مخفي (custom radio) فبندوس على الـ label بتاعه اللي for = اسم النوع
    private By CalendarUnitType (String Type) {
        return By.xpath("//label[@for='" + Type + "']");
    }

    // الكارت بيعرض الاسم بالشكل ده: "اسم الوحدة - 2026" فبندور على النص اللي فيه الاسم
    private By CalendarUnitByName (String Name) {
        // text()[...] عشان Angular بيحط مسافات/comments قبل الاسم فأول text node مبيبقاش هو الاسم
        return By.xpath("//*[text()[contains(normalize-space(.),'" + Name + "')]]");
    }



    public P3_ManageEventsCalendarPage AddCalendarUnitToYear (String Year , String NameAr , String NameEn , String Type) {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,YearActionsButton(Year));
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,AddCalendarUnitButton);
        Utility.SENDKEYS(driver,CalendarUnitNameAr,NameAr);
        Utility.SENDKEYS(driver,CalendarUnitNameEn,NameEn);
        Utility.OPEN_MENU_AND_CLICK(driver,MainCategoryDropdown,FirstDropdownOption);
        Utility.OPEN_MENU_AND_CLICK(driver,SubCategoryDropdown,FirstDropdownOption);
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,CalendarUnitType(Type));
        Utility.OPEN_MENU_AND_CLICK(driver,StartDateField,TodayInCalendar);
        Utility.OPEN_MENU_AND_CLICK(driver,EndDateField,LastEnabledDayInCalendar);
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,SaveButton);
        return this;
    }

    // بعد الحفظ بيحوّل على ملف وحدة التقويم، والوحدة الجديدة ممكن متبقاش أول كارت فبنستنى تبقى موجودة ونعمل scroll لها
    public boolean IsCalendarUnitDisplayed (String Name) {
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.presenceOfElementLocated(CalendarUnitByName(Name)));
        Scrolling.ScrollToElement(driver,CalendarUnitByName(Name));
        LogsUtiles.info("Calendar unit is displayed: " + Name);
        return driver.findElement(CalendarUnitByName(Name)).isDisplayed();
    }
}
