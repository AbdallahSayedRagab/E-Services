package Utilities;

import DriverFactory.DriverFactoryClass;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;

// في src/main (مش test) عشان الصفحات تقدر تستخدمه، والـ TestNG scope = compile في الـ pom
public class Validations {

    private Validations() {
    }

    public static void ValidationTrue (Boolean condition, String message) {
        Assert.assertTrue(condition, message);
    }

    public static void ValidationFalse (Boolean condition, String message) {
        Assert.assertFalse(condition, message);
    }

    public static void ValidateEquals (Object actual, Object expected) {
        Assert.assertEquals(actual, expected);
    }
    public static void ValidateURL( Object expected) throws InterruptedException {
        String CurrentURL = DriverFactoryClass.getdriver().getCurrentUrl();
        Assert.assertEquals(CurrentURL, expected);
    }

    public static void ValidateNotEquals (Object actual, Object expected, String message) {
        Assert.assertNotEquals(actual, expected, message);
    }

    public static void ValidateContains (String actual, String expected) {
        Assert.assertTrue(actual != null && actual.contains(expected),
                "Expected [" + actual + "] to contain [" + expected + "]");
    }

    // بيستنى العنصر يظهر الأول (الصفحة ممكن لسه بتحمل) وبعدين يحكم
    public static void ValidateElementVisible (By Locator) {
        try {
            new WebDriverWait(DriverFactoryClass.getdriver(), Duration.ofSeconds(15))
                    .until(ExpectedConditions.visibilityOfElementLocated(Locator));
        } catch (TimeoutException e) {
            Assert.fail("Element not visible: " + Locator);
        }
    }

    public static void ValidateToast (String expected) {
        ValidateContains(Utility.GET_TOAST_TEXT(DriverFactoryClass.getdriver()), expected);
    }

}
