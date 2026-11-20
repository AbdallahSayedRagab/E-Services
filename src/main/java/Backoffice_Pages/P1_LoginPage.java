package Backoffice_Pages;

import Utilities.LogsUtiles;
import Utilities.Utility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class P1_LoginPage {
    public P1_LoginPage(WebDriver driver){
        this.driver = driver ;
    }
    private WebDriver driver;

    private final By ClickHereButton = By.xpath("//a[contains(@href,'Governmental')]");
    private final By EmailField = By.id("email");
    private final By Password = By.id("password");
    private final By ConfirmLoginByNAFAZ = By.xpath("//button[@value='login']");

    // الخروج لازم يبقى من الاتنين: تطبيق الخدمات الإلكترونية الأول وبعدين البوابة (t-login)
    // لو خرجنا من التطبيق بس البوابة بتفضل داخلة باليوزر القديم واللوجين الجاي يدخل بيه
    private final By AppUserMenu = By.xpath("//app-header//button[contains(@class,'user-open')]");
    private final By AppSignOut = By.xpath("//button[contains(@class,'sign-out')]");
    private final By AppConfirmSignOut = By.cssSelector(".p-confirm-dialog-accept");
    private final By PortalUserMenu = By.id("userActions");
    private final By PortalLogoutLink = By.id("logoutLink");
    private final By PortalConfirmLogout = By.xpath("//div[contains(@class,'nec-logout-popup')]//button[contains(@class,'submit')]");



    public P2_Dashboard Login_As_Admin (String EMAIL , String password) throws InterruptedException {
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,ClickHereButton);
        Utility.SENDKEYS(driver,EmailField,EMAIL);
        Utility.SENDKEYS(driver,Password,password);
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,ConfirmLoginByNAFAZ);
        return new P2_Dashboard( driver);
    }

    // بيخرج من التطبيق (لو الكيس كان جواه) وبعدين من البوابة، ويستنى لينك "Governmental" يظهر
    // لأنه مبيظهرش غير لما البوابة تخرج فعلاً، فلو الخروج منجحش بيفشل هنا مش في الكيس اللي بعده
    public P1_LoginPage Logout () {
        if (!driver.findElements(AppUserMenu).isEmpty()) {
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,AppUserMenu);
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,AppSignOut);
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,AppConfirmSignOut);
            // بعد الخروج من التطبيق البوابة بتفتح (?shouldLogout=True) ونافذة تأكيد خروجها مفتوحة لوحدها
            // ولو دوسنا على قايمة اليوزر النافذة دي بتغطي "تسجيل الخروج" والكليك بيتمنع
            new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.visibilityOfElementLocated(PortalConfirmLogout));
        } else {
            // كيس كان على البوابة بس (زي T1): بنفتح نافذة الخروج من قايمة اليوزر
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,PortalUserMenu);
            Utility.WatingLoader_And_CLICKONELEMENTS(driver,PortalLogoutLink);
        }
        Utility.WatingLoader_And_CLICKONELEMENTS(driver,PortalConfirmLogout);
        new WebDriverWait(driver, Duration.ofSeconds(60)).until(ExpectedConditions.visibilityOfElementLocated(ClickHereButton));
        LogsUtiles.info("Logged out from the e-services app and the portal");
        return this;
    }


}
