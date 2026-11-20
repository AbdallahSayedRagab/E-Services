package Utilities;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class Scrolling {
    private Scrolling() {
    }

    // scrollIntoView لوحده مش بيكفي لو العنصر جوه container ليه scroll خاص بيه (جدول/مودال/سايد منيو)
    // فبنطلع من العنصر لفوق ونعمل scroll لكل container قابل للسكرول (X و Y)
    // وبعدين لو العنصر لسه برة الشاشة أو متغطي (زي الهيدر الثابت) نجيبه في نص الشاشة
    private static final String SMART_SCROLL =
            "var el = arguments[0];" +
            "for (var p = el.parentElement; p && p !== document.body; p = p.parentElement) {" +
            "  var s = getComputedStyle(p);" +
            "  var canY = /(auto|scroll|overlay)/.test(s.overflowY) && p.scrollHeight > p.clientHeight;" +
            "  var canX = /(auto|scroll|overlay)/.test(s.overflowX) && p.scrollWidth > p.clientWidth;" +
            "  if (!canY && !canX) continue;" +
            "  var pr = p.getBoundingClientRect(), er = el.getBoundingClientRect();" +
            "  if (canY) p.scrollTop += (er.top - pr.top) - (p.clientHeight - er.height) / 2;" +
            "  if (canX) p.scrollLeft += (er.left - pr.left) - (p.clientWidth - er.width) / 2;" +
            "}" +
            "var r = el.getBoundingClientRect();" +
            "var inView = r.top >= 0 && r.left >= 0 && r.bottom <= innerHeight && r.right <= innerWidth;" +
            "var top = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2);" +
            "var covered = !top || (top !== el && !el.contains(top));" +
            "if (!inView || covered) el.scrollIntoView({block: 'center', inline: 'nearest'});";

    public static void ScrollToElement(WebDriver driver, By Locator) {
        // Wait for the element to be present in the DOM before trying to find it and
        // scroll
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.presenceOfElementLocated(Locator));
        ScrollToElement(driver, driver.findElement(Locator));
    }

    public static void ScrollToElement(WebDriver driver, WebElement Element) {
        ((JavascriptExecutor) driver).executeScript(SMART_SCROLL, Element);
    }
}
