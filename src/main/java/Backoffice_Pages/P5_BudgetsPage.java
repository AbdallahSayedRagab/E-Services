package Backoffice_Pages;

import Utilities.JsonUtil;
import Utilities.LogsUtiles;
import Utilities.Utility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class P5_BudgetsPage {
    public P5_BudgetsPage (WebDriver driver){
        this.driver = driver ;
    }
    private WebDriver driver;

    // فلتر التقويم والوحدة: نفس الـ component بتاع الهيكل التنظيمي، الأول التقويم والتاني الوحدة
    private final By CalendarFilter = By.xpath("(//app-common-calendar-unit-filter//p-dropdown)[1]");
    private final By UnitFilter = By.xpath("(//app-common-calendar-unit-filter//p-dropdown)[2]");

    // طلب ميزانية جديد هو الـ btn-primary الوحيد في قايمة الميزانيات، وبعده رسالة تأكيد PrimeNG
    private final By NewBudgetRequestButton = By.xpath("//button[contains(@class,'btn') and contains(@class,'btn-primary')]");
    private final By ConfirmYesButton = By.cssSelector(".p-confirm-dialog-accept");

    // فورم الميزانية
    private final By BudgetTotalField = By.xpath("//p-inputnumber[@mode='currency']//input");
    // اضافة بند جديد الأول وتعريف البنود الأساسية التاني (نفس الشكل، فبنمسكهم بالترتيب جوه create-button)
    private final By AddNewItemButton = By.xpath("(//div[contains(@class,'create-button')]/button)[1]");
    private final By DefineBasicItemsButton = By.xpath("(//div[contains(@class,'create-button')]/button)[2]");
    private final By ItemRows = By.xpath("//p-table//tbody/tr[.//button[contains(@class,'edit')]]");
    // نافذة البند (إضافة/تعديل)
    private final By ItemNameAr = By.id("name-ar");
    private final By ItemNameEn = By.id("name-en");
    private final By ItemValue = By.xpath("//p-inputnumber[@id='budget-value']//input");
    private final By SaveItemButton = By.xpath("//div[contains(@class,'p-dialog')]//div[contains(@class,'dialog-footer')]//button[contains(@class,'btn-black')]");
    private final By BudgetDetailsField = By.id("Justification");
    // المرفقات الإجبارية: كل مرفق ليه زرار + (label for=attach-modal-N) ونافذة رفع خاصة بيه
    private final By RequiredAttachmentButtons = By.xpath("//div[contains(@class,'required-attachment-box')]//label[contains(@class,'btn-primary')][starts-with(@for,'attach-modal-')]");
    private final By SaveAsDraftButton = By.xpath("//button[contains(@class,'btn-black')][not(contains(@class,'small'))][not(ancestor::div[contains(@class,'p-dialog')])]");
    private final By SuccessToast = By.cssSelector(".p-toast-message-success");
    private final By ErrorToast = By.cssSelector(".p-toast-message-error");

    // إرسال الطلب (app-start-and-approve-workflow) ونافذة التأكيد بتاعته
    private final By SendRequestButton = By.xpath("//app-start-and-approve-workflow/button");
    private final By ConfirmSendButton = By.xpath("//app-start-and-approve-workflow//div[contains(@class,'p-dialog-footer')]//button[contains(@class,'btn-primary')]");
    // تقييم الخدمة بيظهر بعد الإرسال ومش محتاجينه، فبندوس "ليس الآن"
    private final By RatingNotNowButton = By.xpath("//app-service-evaluation//button[contains(@class,'btn-outline-secondary')]");

    private By DropdownOptionContains (String Text) {
        return By.xpath("//li[@role='option'][contains(@aria-label,'" + Text + "')]");
    }

    private By AttachmentButton (String For) {
        return By.xpath("//label[contains(@class,'btn-primary')][@for='" + For + "']");
    }

    // ملف المرفق جوه نافذة المرفق نفسه (اللي بعد الـ checkbox بتاعه)
    private By AttachmentFileInput (String For) {
        return By.xpath("//input[@id='" + For + "']/following-sibling::div[contains(@class,'attachment-modal')]//input[@type='file']");
    }

    private By SaveAttachmentButton (String For) {
        return By.xpath("//input[@id='" + For + "']/following-sibling::div[contains(@class,'attachment-modal')]//div[contains(@class,'attachment-modal-footer')]//button[contains(@class,'btn-black')]");
    }

    private By AttachmentUploaded (String For) {
        return By.xpath("//input[@id='" + For + "']/following-sibling::div[contains(@class,'attachment-modal')]//input[@formcontrolname='assets'][string-length(@value)>0]");
    }



    public P5_BudgetsPage FilterByCalendarUnit (String Year , String UnitName) {
        Utility.OPEN_MENU_AND_CLICK(driver,CalendarFilter,DropdownOptionContains(Year));
        Utility.OPEN_MENU_AND_CLICK(driver,UnitFilter,DropdownOptionContains(UnitName));
        return this;
    }

    // بيعمل طلب ميزانية: الإجمالي + البنود الأساسية + بند جديد، وكل البنود بالتساوي عشان المتبقي يبقى صفر
    public P5_BudgetsPage CreateBudgetRequest (long Total , String NewItemAr , String NewItemEn , String Details , String FilePath) {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,NewBudgetRequestButton);
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,ConfirmYesButton);
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.urlContains("/create"));
        SaveRequestNumber();
        Utility.CLEAR_AND_SENDKEYS(driver,BudgetTotalField,String.valueOf(Total));

        Utility.WatingLoader_And_CLICKONELEMENTS(driver,DefineBasicItemsButton);
        int BasicItems = new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.numberOfElementsToBeMoreThan(ItemRows, 0)).size();
        long EachItem = Total / (BasicItems + 1);

        Utility.WatingLoader_And_CLICKONELEMENTS(driver,AddNewItemButton);
        Utility.SENDKEYS(driver,ItemNameAr,NewItemAr);
        Utility.SENDKEYS(driver,ItemNameEn,NewItemEn);
        Utility.SENDKEYS(driver,ItemValue,String.valueOf(EachItem));
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,SaveItemButton);
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.numberOfElementsToBe(ItemRows, BasicItems + 1));

        // البنود الأساسية بتيجي بصفر، فلازم نعدلها كلها بنفس القيمة
        for (int i = 1; i <= BasicItems; i++) {
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,By.xpath("(//p-table//tbody/tr//button[contains(@class,'edit')])[" + i + "]"));
            Utility.CLEAR_AND_SENDKEYS(driver,ItemValue,String.valueOf(EachItem));
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,SaveItemButton);
            new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions.invisibilityOfElementLocated(SaveItemButton));
        }
        LogsUtiles.info("Budget items: " + (BasicItems + 1) + " x " + EachItem);

        Utility.SENDKEYS(driver,BudgetDetailsField,Details);
        AddRequiredAttachments(FilePath);
        return this;
    }

    // كل المرفقات الإجبارية بنفس ملف التيست اللي في TestData
    public P5_BudgetsPage AddRequiredAttachments (String FilePath) {
        List<String> Attachments = driver.findElements(RequiredAttachmentButtons).stream().map(e -> e.getAttribute("for")).toList();
        for (String For : Attachments) {
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,AttachmentButton(For));
            driver.findElement(AttachmentFileInput(For)).sendKeys(FilePath);
            // الملف بيترفع الأول وبعدين الـ assets بيتملى بالـ id بتاعه
            new WebDriverWait(driver, Duration.ofSeconds(60)).until(ExpectedConditions.presenceOfElementLocated(AttachmentUploaded(For)));
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,SaveAttachmentButton(For));
            LogsUtiles.info("Attachment uploaded to " + For);
        }
        return this;
    }

    // بعد الحفظ بيفتح الطلب نفسه (view) وعليه زرار إرسال الطلب، فمش محتاجين نرجع للقايمة
    public P5_BudgetsPage SaveAsDraft () {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,SaveAsDraftButton);
        // الحفظ نجح لما يفتح صفحة العرض (رسالة النجاح ممكن تختفي وإحنا مستنيين اللودر)
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/view/"), ExpectedConditions.visibilityOfElementLocated(ErrorToast)));
        if (!driver.getCurrentUrl().contains("/view/"))
            throw new AssertionError("Saving the draft failed: " + driver.findElement(ErrorToast).getText());
        return this;
    }

    // رسالة النجاح بتظهر وتختفي وإحنا مستنيين اللودر بعد التأكيد، فبنعتبر الإرسال نجح لو ظهرت أو ظهر التقييم أو زرار الإرسال اختفى
    // ولو ظهرت رسالة خطأ بنفشل بيها على طول
    public boolean SendRequest () {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,SendRequestButton);
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,ConfirmSendButton);
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(SuccessToast),
                ExpectedConditions.elementToBeClickable(RatingNotNowButton),
                ExpectedConditions.invisibilityOfElementLocated(SendRequestButton),
                ExpectedConditions.visibilityOfElementLocated(ErrorToast)));
        if (!driver.findElements(ErrorToast).isEmpty())
            throw new AssertionError("Sending the request failed: " + driver.findElement(ErrorToast).getText());
        SkipRating();
        return true;
    }

    // لو ظهر التقييم بندوس ليس الآن، ولو مظهرش نكمل عادي
    private void SkipRating () {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(RatingNotNowButton));
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,RatingNotNowButton);
        } catch (org.openqa.selenium.TimeoutException e) {
            LogsUtiles.info("Rating dialog didn't appear");
        }
    }

    // الرقم المرجعي (EBE-...) بيظهر أول ما الطلب يتعمل، وبنحفظه عشان المعتمدين يدوروا عليه في مهامي
    private void SaveRequestNumber () {
        String Number = new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
            Matcher M = Pattern.compile("EBE-[0-9]+-[0-9]+").matcher(d.findElement(By.tagName("body")).getText());
            return M.find() ? M.group() : null;
        });
        JsonUtil.writeValue("LastBudgetRequestNumber", Number);
    }
}
