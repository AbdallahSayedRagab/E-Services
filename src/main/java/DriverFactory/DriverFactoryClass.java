package DriverFactory;

import Utilities.DataUtiles;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverFactoryClass {
    public static ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    //    @Step ("Create driver instance on: {BROWSER}")
    public static void SetupDriver (String BROWSER) {
        switch (BROWSER.toLowerCase())
        {
            case "chrome": {
                ChromeOptions Options = new ChromeOptions();
                // على الـ CI مفيش شاشة، فلازم headless وحجم شاشة ثابت عشان العناصر تبان زي اللوكال
                if (IsHeadless()) {
                    Options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                            "--disable-gpu", "--window-size=1920,1080");
                } else {
                    Options.addArguments("--start-maximized");
                }
                Options.addArguments("--incognito",
                        // الـ scale بيختلف من بروجكت للتاني (COC = 0.75 / Matloob = 1) فبيتقرا من الانفيرومنت
                        "--force-device-scale-factor=" + DataUtiles.getPropertyValue("environment", "SCALE_FACTOR", "1"),
                        "--disable-features=PasswordLeakDetection,Translate,TranslateUI",
                        "--disable-notifications", "--disable-popup-blocking",
                        "--no-default-browser-check", "--no-first-run",
                        "--disable-search-engine-choice-screen",
                        // عشان الـ animations متقفش لو البراوزر مش في الواجهة (القوايم بتفضل opacity 0)
                        "--disable-backgrounding-occluded-windows",
                        "--disable-renderer-backgrounding",
                        "--disable-background-timer-throttling");
                // نشيل بار "Chrome is being controlled by automated software" وبوب أبات الباسورد عشان متغطيش العناصر
                Options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
                Map<String, Object> Prefs = new HashMap<>();
                Prefs.put("credentials_enable_service", false);
                Prefs.put("profile.password_manager_enabled", false);
                Prefs.put("profile.password_manager_leak_detection", false);
                Prefs.put("profile.default_content_setting_values.notifications", 2);
                Prefs.put("translate.enabled", false);
                Prefs.put("autofill.profile_enabled", false);
                Prefs.put("autofill.credit_card_enabled", false);
                Prefs.put("download.prompt_for_download", false);
                Options.setExperimentalOption("prefs", Prefs);
                driverThreadLocal.set(new ChromeDriver(Options));
                break;
            }

            case "edge": {
                EdgeOptions Options = new EdgeOptions();
                if (IsHeadless()) Options.addArguments("--headless=new", "--window-size=1920,1080");
                else Options.addArguments("--start-maximized");
                Options.addArguments("-inprivate");
                driverThreadLocal.set(new EdgeDriver(Options));
                break;
            }

            default: {
                FirefoxOptions Options = new FirefoxOptions();
                if (IsHeadless()) Options.addArguments("-headless");
                else Options.addArguments("--kiosk");
                Options.addPreference("layout.css.devPixelsPerPx", "0.80");
                driverThreadLocal.set(new FirefoxDriver(Options));
            }
        }
        // اسم ونسخة البراوزر بيظهروا في هيدر التقرير
        Capabilities Caps = ((HasCapabilities) getdriver()).getCapabilities();
        BrowserName = Caps.getBrowserName();
        BrowserVersion = Caps.getBrowserVersion();
    }

    public static String BrowserName;
    public static String BrowserVersion;

    // headless لو اتبعت -Dheadless=true أو لو شغالين على Bitbucket/Jenkins (مفيش شاشة هناك)
    public static boolean IsHeadless () {
        return Boolean.parseBoolean(System.getProperty("headless", "false"))
                || System.getenv("CI") != null
                || System.getenv("JENKINS_URL") != null
                || System.getenv("JENKINS_HOME") != null;
    }

    public static WebDriver getdriver (){
        return driverThreadLocal.get();
    }

    public static void Quit (){
        if (getdriver() != null) getdriver().quit();
        driverThreadLocal.remove();
    }
}
