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

// طلبات المناقلة: بننقل مبلغ من بند لبند جوه نفس الميزانية المعتمدة والإجمالي مبيتغيرش
// بننقل من البند اللي ضفناه بإيدينا في طلب الميزانية لأول بند في قايمة "المنقول إليه"
public class P8_TransferRequestsPage {
    public P8_TransferRequestsPage (WebDriver driver){
        this.driver = driver ;
    }
    private WebDriver driver;

    // نفس فلتر التقويم والوحدة بتاع الميزانيات
    private final By CalendarFilter = By.xpath("(//app-common-calendar-unit-filter//p-dropdown)[1]");
    private final By UnitFilter = By.xpath("(//app-common-calendar-unit-filter//p-dropdown)[2]");

    // طلب مناقلة جديد هو الـ btn-primary الوحيد في القايمة، وبعده رسالة تأكيد PrimeNG
    private final By NewTransferRequestButton = By.xpath("//button[contains(@class,'btn-primary')][not(ancestor::div[contains(@class,'p-dialog')])]");
    private final By ConfirmYesButton = By.cssSelector(".p-confirm-dialog-accept");

    // اضافة بند جديد هو الـ btn-primary اللي برا نافذة البند وبرا المرفقات (المرفقات فيها أزرار btn-primary صغيرة)
    private final By AddItemButton = By.xpath("//app-create-update-transfer-request//button[contains(@class,'btn-primary')][not(ancestor::app-create-budget-item)][not(ancestor::app-attachment-control)]");
    private final By ItemRows = By.xpath("//app-create-update-transfer-request//p-table//tbody/tr[.//button]");
    // نافذة "بنود الميزانية": الأول المنقول منه والتاني المنقول إليه (التاني بيستبعد البند اللي اخترناه في الأول)
    private final By SourceItemDropdown = By.xpath("(//app-create-budget-item//p-dropdown)[1]");
    private final By DestinationItemDropdown = By.xpath("(//app-create-budget-item//p-dropdown)[2]");
    private final By FirstDropdownOption = By.xpath("(//li[@role='option'])[1]");
    private final By TransferAmountField = By.xpath("//app-create-budget-item//p-inputnumber//input");
    private final By SaveItemButton = By.xpath("//app-create-budget-item//div[contains(@class,'dialog-footer')]//button[contains(@class,'btn-black')]");
    private final By JustificationField = By.id("Justification");
    private final By SaveAsDraftButton = By.xpath("//button[contains(@class,'btn-black')][not(contains(@class,'small'))][not(ancestor::div[contains(@class,'p-dialog')])]");
    private final By ErrorToast = By.cssSelector(".p-toast-message-error");

    private By DropdownOptionContains (String Text) {
        return By.xpath("//li[@role='option'][contains(@aria-label,'" + Text + "')]");
    }



    public P8_TransferRequestsPage FilterByCalendarUnit (String Year , String UnitName) {
        Utility.OPEN_MENU_AND_CLICK(driver,CalendarFilter,DropdownOptionContains(Year));
        Utility.OPEN_MENU_AND_CLICK(driver,UnitFilter,DropdownOptionContains(UnitName));
        return this;
    }

    public P8_TransferRequestsPage CreateTransferRequest (String FromItem , long Amount , String Justification , String FilePath) {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,NewTransferRequestButton);
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,ConfirmYesButton);
        // المناقلة بتفتح على edit مش create (المسودة بتتعمل أول ما نأكد)
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.urlMatches(".*/transfer-requests/(create|edit).*"));
        SaveRequestNumber();

        Utility.WatingLoader_And_CLICKONELEMENTS(driver,AddItemButton);
        Utility.OPEN_MENU_AND_CLICK(driver,SourceItemDropdown,DropdownOptionContains(FromItem));
        Utility.OPEN_MENU_AND_CLICK(driver,DestinationItemDropdown,FirstDropdownOption);
        Utility.SENDKEYS(driver,TransferAmountField,String.valueOf(Amount));
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,SaveItemButton);
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.numberOfElementsToBe(ItemRows, 1));
        LogsUtiles.info("Transfer item: " + Amount + " from " + FromItem);

        Utility.SENDKEYS(driver,JustificationField,Justification);
        // المرفقات نفس component الميزانية، وبعضها بيتملى لوحده من الميزانية فبنرفع على الفاضي بس
        new P5_BudgetsPage(driver).AddRequiredAttachments(FilePath);
        return this;
    }

    // الحفظ نجح لما يفتح صفحة العرض (رسالة النجاح ممكن تختفي وإحنا مستنيين اللودر)
    public P8_TransferRequestsPage SaveAsDraft () {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,SaveAsDraftButton);
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

    // الرقم المرجعي للمناقلة (EBT-...) بيظهر أول ما الطلب يتعمل، وبنحفظه عشان المعتمدين يدوروا عليه في مهامي
    private void SaveRequestNumber () {
        String Number = new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
            Matcher M = Pattern.compile("EBT-[0-9]+-[0-9]+").matcher(d.findElement(By.tagName("body")).getText());
            return M.find() ? M.group() : null;
        });
        JsonUtil.writeValue("LastTransferRequestNumber", Number);
    }
}
