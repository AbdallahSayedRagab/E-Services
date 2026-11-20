package Backoffice_Pages;

import Utilities.LogsUtiles;
import Utilities.Utility;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

// الرئيسية: فيها مهامي وهنضيف فيها أي حاجة تخص الرئيسية بعد كده
public class P6_HomePage {
    public P6_HomePage (WebDriver driver){
        this.driver = driver ;
    }
    private WebDriver driver;

    // مهامي هي app-task-list، والصفوف clickable، والصفحات بالـ paginator
    private final By MyTasksRows = By.xpath("//app-task-list//tbody/tr[contains(@class,'clickable')]");
    private final By MyTasksNextPage = By.xpath("//app-task-list//button[contains(@class,'p-paginator-next')]");

    // أزرار المهمة جوه app-budget-request-tasks أو app-supplement-request-tasks (اعتماد / تعيين الطلب لي هو الـ btn-primary في actions)
    private final By TaskMainAction = By.xpath("//*[self::app-budget-request-tasks or self::app-supplement-request-tasks]//div[contains(@class,'actions')]/button[contains(@class,'btn-primary')]");
    private final By TaskNotes = By.id("notes");
    private final By TaskConfirmButton = By.xpath("//app-task-confirmation-dialog//div[contains(@class,'p-dialog-footer')]//button[contains(@class,'btn-primary')]");
    private final By SuccessToast = By.cssSelector(".p-toast-message-success");
    private final By ErrorToast = By.cssSelector(".p-toast-message-error");

    // موظف الميزانية لازم يكمّل بيانات كل بند (رقم الارتباط المالي) قبل الاعتماد
    private final By BudgetItemEditButtons = By.xpath("//p-table//tbody/tr//button[contains(@class,'edit')]");
    private final By FundedProgramField = By.xpath("//div[contains(@class,'p-dialog')]//input[@formcontrolname='fundedProgram']");
    private final By SaveBudgetItemButton = By.xpath("//div[contains(@class,'p-dialog')]//div[contains(@class,'dialog-footer')]//button[contains(@class,'btn-black')]");

    // المهمة في مهامي بالرقم المرجعي (عمود رقم الطلب)
    private By TaskByRequestNumber (String RequestNumber) {
        return By.xpath("//app-task-list//tr[contains(@class,'clickable')][td[normalize-space()='" + RequestNumber + "']]");
    }



    // بيدور على الطلب في مهامي صفحة صفحة لحد ما يلاقيه ويفتحه
    public P6_HomePage OpenTask (String RequestNumber) {
        new WebDriverWait(driver, Duration.ofSeconds(60)).until(ExpectedConditions.presenceOfElementLocated(MyTasksRows));
        for (int Page = 1; Page <= 20; Page++) {
            if (!driver.findElements(TaskByRequestNumber(RequestNumber)).isEmpty()) {
                ClickTask(TaskByRequestNumber(RequestNumber));
                LogsUtiles.info("Task opened: " + RequestNumber + " (page " + Page + ")");
                return this;
            }
            List<WebElement> Next = driver.findElements(MyTasksNextPage);
            if (Next.isEmpty() || !Next.get(0).isEnabled() || Next.get(0).getAttribute("class").contains("p-disabled")) break;
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,MyTasksNextPage);
        }
        throw new AssertionError("Task " + RequestNumber + " not found in My Tasks");
    }

    // صفحة الطلب بتحمّل بياناتها الأول وبعدين اللينك يتغير (ممكن ياخد أكتر من 5 ثواني)، فلو عدنا الكليك بدري الصف بيكون اختفى
    // فبنعيد الكليك بس لو الصف لسه موجود (يعني الكليك ضاع)، ولو اختفى بنستنى التحميل يكمل
    private void ClickTask (By Row) {
        for (int i = 0; i < 3; i++) {
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,Row);
            try {
                new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.urlContains("taskId="));
                return;
            } catch (TimeoutException e) {
                if (driver.findElements(Row).isEmpty()) {
                    new WebDriverWait(driver, Duration.ofSeconds(40)).until(ExpectedConditions.urlContains("taskId="));
                    return;
                }
                LogsUtiles.info("Task didn't open, clicking again " + Row);
            }
        }
        throw new AssertionError("Task row didn't open after 3 clicks: " + Row);
    }

    // تعيين الطلب لي (موظف الميزانية): بعدها المهمة بتتقفل وبتتفتح مهمة جديدة في مهامي
    public P6_HomePage AssignTaskToMe () {
        ConfirmTaskAction("");
        return this;
    }

    // رقم الارتباط المالي لكل بند مختلف عن التاني: البند الأول 1 والتاني 2 وهكذا
    public P6_HomePage CompleteBudgetItemsData () {
        int Items = driver.findElements(BudgetItemEditButtons).size();
        for (int i = 1; i <= Items; i++) {
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,By.xpath("(//p-table//tbody/tr//button[contains(@class,'edit')])[" + i + "]"));
            Utility.CLEAR_AND_SENDKEYS(driver,FundedProgramField,String.valueOf(i));
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,SaveBudgetItemButton);
            new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.visibilityOfElementLocated(SuccessToast));
            new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.invisibilityOfElementLocated(SuccessToast));
        }
        return this;
    }

    // اعتماد / إرسال للاعتماد: كلها نفس الزرار ونفس نافذة التأكيد
    public boolean ApproveTask (String Notes) {
        ConfirmTaskAction(Notes);
        return true;
    }

    private void ConfirmTaskAction (String Notes) {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,TaskMainAction);
        if (!Notes.isEmpty()) Utility.SENDKEYS(driver,TaskNotes,Notes);
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,TaskConfirmButton);
        // بعد النجاح بيرجع للرئيسية، ورسالة النجاح ساعات بتختفي قبل ما نلحقها (تعيين الطلب لي في التعزيز)
        // فبنستنى الرجوع للرئيسية، ولو ظهرت رسالة خطأ بنفشل بيها على طول
        new WebDriverWait(driver, Duration.ofSeconds(60)).until(ExpectedConditions.or(
                ExpectedConditions.urlContains("#/dashboard"), ExpectedConditions.visibilityOfElementLocated(ErrorToast)));
        if (!driver.getCurrentUrl().contains("#/dashboard"))
            throw new AssertionError("Task action failed: " + driver.findElement(ErrorToast).getText());
    }
}
