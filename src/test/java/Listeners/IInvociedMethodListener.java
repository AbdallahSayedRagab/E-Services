package Listeners;

import Utilities.LogsUtiles;
import Utilities.Utility;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.util.Base64;

import static DriverFactory.DriverFactoryClass.getdriver;

public class IInvociedMethodListener implements IInvokedMethodListener {


    // بنصور هنا مش في onTestFailure عشان ده بيتنادى قبل @AfterMethod (البراوزر لسه مفتوح)
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        WebDriver driver = getdriver();
        if (testResult.getStatus()==ITestResult.FAILURE && driver != null) {
            LogsUtiles.info("Test-Case:" + testResult.getName() + " Failed");

            try {
                Utility.SCREENSHOT(driver,testResult.getName());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // الصورة بتتحفظ مع النتيجة عشان Result-Report يعرضها في تفاصيل الكيس
            try {
                byte[] Shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                testResult.setAttribute("Screenshot", Base64.getEncoder().encodeToString(Shot));
            } catch (Exception e) {
                LogsUtiles.warn("Couldn't take failure screenshot for the report: " + e.getMessage());
            }
        }
    }
}
