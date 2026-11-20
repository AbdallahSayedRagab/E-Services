package Backoffice_Pages;

import Utilities.LogsUtiles;
import Utilities.Scrolling;
import Utilities.Utility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class P4_OrgStructurePage {
    public P4_OrgStructurePage (WebDriver driver){
        this.driver = driver ;
    }
    private WebDriver driver;

    // فلتر التقويم والوحدة: الاتنين جوه app-common-calendar-unit-filter، الأول التقويم والتاني الوحدة
    private final By CalendarFilter = By.xpath("(//app-common-calendar-unit-filter//p-dropdown)[1]");
    private final By UnitFilter = By.xpath("(//app-common-calendar-unit-filter//p-dropdown)[2]");

    // الشجرة متكررة مرتين في الصفحة (نسخة للطباعة مخفية)، فبناخد اللي جوه bordered-box عشان دي اللي ظاهرة
    // node-1 هي الوحدة نفسها (المستوى الأول) وكل اليوزرز بيتضافوا تحتها
    private final By RootNodeAddButton = By.xpath("//div[contains(@class,'bordered-box')]//div[contains(@class,'node-1')]//button[contains(@class,'treeDropdown-btn')]");

    // فورم الوحدة التنظيمية (nec-form): الحقول ملهاش id فبنمسكها بترتيبها بعد الـ labels الإجبارية
    private final By NodeNameAr = By.xpath("(//div[contains(@class,'nec-form')]//label[contains(@class,'required')]/following-sibling::input)[1]");
    private final By NodeNameEn = By.xpath("(//div[contains(@class,'nec-form')]//label[contains(@class,'required')]/following-sibling::input)[2]");
    // "مستخدم بالهيكل التنظيمي والنظام" هو اللي بيظهر الدور الوظيفي واسم المسؤول
    private final By SystemUserRadio = By.xpath("//label[@for='desition1']");
    private final By RoleDropdown = By.xpath("//div[contains(@class,'nec-form')]//label[contains(@class,'required')]/following-sibling::p-dropdown[not(@optionlabel)]");
    // اسم المسؤول هو الـ dropdown الوحيد اللي optionlabel بتاعه fullName، والليست بتتفلتر حسب الدور
    private final By ResponsibleDropdown = By.xpath("//div[contains(@class,'nec-form')]//p-dropdown[@optionlabel='fullName']");
    private final By DropdownSearchField = By.xpath("//input[contains(@class,'p-dropdown-filter')]");
    private final By FirstDropdownOption = By.xpath("(//li[@role='option'])[1]");
    // صلاحية الاعتماد للطلبات بتظهر لبعض الأدوار بس (مكتب إدارة الموسم / مدير وحدة التقويم)
    // والـ checkbox اللي في أول القايمة بيختار كل الصلاحيات مرة واحدة
    private final By ApprovalPermissionsDropdown = By.xpath("//div[contains(@class,'nec-form')]//p-multiselect");
    private final By SelectAllPermissions = By.xpath("//div[contains(@class,'p-multiselect-panel')]//div[contains(@class,'p-multiselect-header')]//div[contains(@class,'p-checkbox-box')]");
    private final By ClosePermissionsList = By.xpath("//div[contains(@class,'p-multiselect-panel')]//button[contains(@class,'p-multiselect-close')]");
    private final By SaveNodeButton =By.xpath("//div[contains(@class,'nec-form')]//button[contains(@class,'btn-primary')]");
    // حفظ الهيكل التنظيمي بيظهر بعد أول وحدة، وهو الـ btn-primary اللي برة فورم الوحدة
    private final By SaveOrgStructureButton = By.xpath("//app-organizational-structure//button[contains(@class,'btn-primary')][not(ancestor::div[contains(@class,'nec-form')])]");
    // رسالة "هل تريد حفظ الهيكل التنظيمي؟": نعم ليها class ثابت في PrimeNG
    private final By ConfirmYesButton = By.cssSelector(".p-confirm-dialog-accept");
    private final By SuccessToast = By.cssSelector(".p-toast-message-success");

    // الاختيار من الليست بالاسم اللي جاي من الجيسون (التقويم بالسنة، الوحدة باسمها، الدور باسمه)
    private By DropdownOptionContains (String Text) {
        return By.xpath("//li[@role='option'][contains(@aria-label,'" + Text + "')]");
    }

    private By NodeByName (String Name) {
        return By.xpath("//div[contains(@class,'bordered-box')]//div[contains(@class,'node-label')][.//text()[contains(normalize-space(.),'" + Name + "')]]");
    }



    public P4_OrgStructurePage FilterByCalendarUnit (String Year , String UnitName) {
        Utility.OPEN_MENU_AND_CLICK(driver,CalendarFilter,DropdownOptionContains(Year));
        Utility.OPEN_MENU_AND_CLICK(driver,UnitFilter,DropdownOptionContains(UnitName));
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.elementToBeClickable(RootNodeAddButton));
        return this;
    }

    // بيضيف يوزر كوحدة تنظيمية تحت الوحدة: الاسم عربي وانجليزي + مستخدم بالنظام + الدور + اسم المسؤول
    public P4_OrgStructurePage AddUserToUnit (String NameAr , String NameEn , String Role , String UserName) {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,RootNodeAddButton);
        Utility.SENDKEYS(driver,NodeNameAr,NameAr);
        Utility.SENDKEYS(driver,NodeNameEn,NameEn);
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,SystemUserRadio);
        Utility.OPEN_MENU_AND_CLICK(driver,RoleDropdown,DropdownOptionContains(Role));
        // اسم المسؤول بنكتبه في البحث وناخد أول نتيجة عشان الليست فيها أسماء كتير شبه بعض
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,ResponsibleDropdown);
        Utility.SENDKEYS(driver,DropdownSearchField,UserName);
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,FirstDropdownOption);
        SelectAllApprovalPermissionsIfExist();
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,SaveNodeButton);
        // بنستنى "تم حفظ الوحدة التنظيمية بنجاح" قبل ما نضيف اللي بعده
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.visibilityOfElementLocated(SuccessToast));
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.invisibilityOfElementLocated(SuccessToast));
        LogsUtiles.info("Org unit added: " + NameAr + " / " + UserName);
        return this;
    }

    // لو الدور ليه صلاحيات اعتماد بنختارها كلها، ولو مالوش بنكمل عادي
    private void SelectAllApprovalPermissionsIfExist () {
        if (driver.findElements(ApprovalPermissionsDropdown).isEmpty()) return;
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,ApprovalPermissionsDropdown);
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,SelectAllPermissions);
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.attributeToBe(SelectAllPermissions, "aria-checked", "true"));
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,ClosePermissionsList);
        LogsUtiles.info("All approval permissions selected");
    }

    public P4_OrgStructurePage SaveOrgStructure () {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,SaveOrgStructureButton);
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,ConfirmYesButton);
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.visibilityOfElementLocated(SuccessToast));
        return this;
    }

    public boolean IsNodeDisplayed (String Name) {
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.presenceOfElementLocated(NodeByName(Name)));
        Scrolling.ScrollToElement(driver,NodeByName(Name));
        return driver.findElement(NodeByName(Name)).isDisplayed();
    }
}
