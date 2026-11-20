package Backoffice_Pages;

import Utilities.JsonUtil;
import Utilities.LogsUtiles;
import Utilities.Utility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// طلبات التعزيز: بتشتغل على الميزانيات المعتمدة بس، وبنعزز البند اللي ضفناه بإيدينا في طلب الميزانية
public class P7_SupplementRequestsPage {
    public P7_SupplementRequestsPage (WebDriver driver){
        this.driver = driver ;
    }
    private WebDriver driver;

    // نفس فلتر التقويم والوحدة بتاع الميزانيات
    private final By CalendarFilter = By.xpath("(//app-common-calendar-unit-filter//p-dropdown)[1]");
    private final By UnitFilter = By.xpath("(//app-common-calendar-unit-filter//p-dropdown)[2]");

    // طلب تعزيز جديد هو الـ btn-primary الوحيد في القايمة، وبعده رسالة تأكيد PrimeNG
    private final By NewSupplementRequestButton = By.xpath("//button[contains(@class,'btn-primary')][not(ancestor::div[contains(@class,'p-dialog')])]");
    private final By ConfirmYesButton = By.cssSelector(".p-confirm-dialog-accept");

    // فورم التعزيز: مبلغ التعزيز الكلي هو الـ p-inputnumber اللي برا نافذة البند
    private final By SupplementAmountField = By.xpath("//app-create-update-supplement-request//p-inputnumber[not(ancestor::app-create-supplement-item)]//input");
    private final By AddItemButton = By.xpath("//app-create-update-supplement-request//button[contains(@class,'btn-primary')][not(ancestor::app-create-supplement-item)]");
    private final By ItemRows = By.xpath("//app-create-update-supplement-request//p-table//tbody/tr[.//button[contains(@class,'edit')]]");
    // نافذة توزيع التعزيز على البنود: بنختار بند موجود (مش بند جديد) وبنكتب المبلغ
    private final By ItemDropdown = By.xpath("//app-create-supplement-item//p-dropdown");
    private final By ItemValue = By.xpath("//app-create-supplement-item//p-inputnumber[@id='budget-value']//input");
    private final By SaveItemButton = By.xpath("//app-create-supplement-item//div[contains(@class,'dialog-footer')]//button[contains(@class,'btn-black')]");
    private final By JustificationField = By.id("Justification");
    private final By SaveAsDraftButton = By.xpath("//button[contains(@class,'btn-black')][not(contains(@class,'small'))][not(ancestor::div[contains(@class,'p-dialog')])]");
    private final By ErrorToast = By.cssSelector(".p-toast-message-error");

    private By DropdownOptionContains (String Text) {
        return By.xpath("//li[@role='option'][contains(@aria-label,'" + Text + "')]");
    }



    public P7_SupplementRequestsPage FilterByCalendarUnit (String Year , String UnitName) {
        Utility.OPEN_MENU_AND_CLICK(driver,CalendarFilter,DropdownOptionContains(Year));
        Utility.OPEN_MENU_AND_CLICK(driver,UnitFilter,DropdownOptionContains(UnitName));
        return this;
    }

    // المبلغ كله على بند واحد، فالمتبقي من التعزيز بيبقى صفر
    public P7_SupplementRequestsPage CreateSupplementRequest (long Amount , String ItemName , String Justification , String FilePath) {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,NewSupplementRequestButton);
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,ConfirmYesButton);
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.urlContains("/create"));
        SaveRequestNumber();
        Utility.CLEAR_AND_SENDKEYS(driver,SupplementAmountField,String.valueOf(Amount));

        Utility.WatingLoader_And_CLICKONELEMENTS(driver,AddItemButton);
        Utility.OPEN_MENU_AND_CLICK(driver,ItemDropdown,DropdownOptionContains(ItemName));
        Utility.SENDKEYS(driver,ItemValue,String.valueOf(Amount));
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,SaveItemButton);
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.numberOfElementsToBe(ItemRows, 1));
        LogsUtiles.info("Supplement item: " + ItemName + " = " + Amount);

        Utility.SENDKEYS(driver,JustificationField,Justification);
        // المرفقات الإجبارية نفس component الميزانية بالظبط
        new P5_BudgetsPage(driver).AddRequiredAttachments(FilePath);
        return this;
    }

    // بعد الحفظ بيفتح صفحة العرض وعليها زرار إرسال الطلب
    public P7_SupplementRequestsPage SaveAsDraft () {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,SaveAsDraftButton);
        // الحفظ نجح لما يفتح صفحة العرض (رسالة النجاح ممكن تختفي وإحنا مستنيين اللودر)
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/view/"), ExpectedConditions.visibilityOfElementLocated(ErrorToast)));
        if (!driver.getCurrentUrl().contains("/view/"))
            throw new AssertionError("Saving the draft failed: " + driver.findElement(ErrorToast).getText());
        return this;
    }

    // الإرسال والتقييم نفس app-start-and-approve-workflow بتاع الميزانية
    public boolean SendRequest () {
        return new P5_BudgetsPage(driver).SendRequest();
    }

    // الرقم المرجعي للتعزيز (EBS-...) بيظهر أول ما الطلب يتعمل، وبنحفظه عشان المعتمدين يدوروا عليه في مهامي
    private void SaveRequestNumber () {
        String Number = new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
            Matcher M = Pattern.compile("EBS-[0-9]+-[0-9]+").matcher(d.findElement(By.tagName("body")).getText());
            return M.find() ? M.group() : null;
        });
        JsonUtil.writeValue("LastSupplementRequestNumber", Number);
    }
}
