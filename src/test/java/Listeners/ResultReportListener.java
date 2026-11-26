package Listeners;

import DriverFactory.DriverFactoryClass;
import Utilities.LogsUtiles;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.openqa.selenium.BuildInfo;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.internal.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// بعد ما الرن يخلص بيكتب test-outputs/Result-Report.html بنفس شكل تقرير "نتيجة الاختبار" (الداش بورد الغامق + انترو اللمبة واللوجو)
// التصميم كله (HTML + CSS + JS) ثابت في src/main/resources/report/ResultReport.html،
// والجافا بتبني الداتا بس كـ JSON وبتحطها مكان __REPORT_DATA__، فأي تعديل في الشكل بيبقى في ملف واحد
public class ResultReportListener implements ISuiteListener {

    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(CAIRO);
    private static final DateTimeFormatter SHORT_TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(CAIRO);

    // كل سطر لوج بيتسجل هنا وقت الرن، وبعدين كل كيس فاشل بياخد السطور اللي حصلت بين بدايته ونهايته
    private static final List<LogLine> Logs = new ArrayList<>();
    private static long SuiteStart = System.currentTimeMillis();

    static {
        // بيتسجل أول ما الكلاس يتحمّل (قبل أول تيست) عشان منضيعش لوج الكيس الأول
        try {
            LoggerContext Ctx = (LoggerContext) LogManager.getContext(false);
            Configuration Config = Ctx.getConfiguration();
            CaptureAppender Appender = new CaptureAppender();
            Appender.start();
            Config.addAppender(Appender);
            Config.getRootLogger().addAppender(Appender, null, null);
            Ctx.updateLoggers();
        } catch (Exception e) {
            System.err.println("Result report: couldn't capture logs - " + e.getMessage());
        }
    }

    // الدورة كلها كلاس واحد و<test> واحد، فبنقسمها لوحدات حسب الـ priority عشان لوحة "حالات الاختبار" يبقى ليها معنى
    // (أول priority في المرحلة ← الاسم إنجليزي/عربي). أي كلاس تاني بيتقسم بالـ <test> بتاعه في الـ xml زي العادي
    private static final String PHASED_CLASS = "Tests.T1_BudgetCases";
    private static final Object[][] PHASES = {
            {1, "Cycle setup", "تجهيز الدورة"},
            {4, "Budget request", "طلب الميزانية"},
            {9, "Supplement request", "طلب التعزيز"},
            {14, "Transfer request", "طلب المناقلة"}};

    // القيم اللي بتظهر في "بيانات الاختبار" (المفتاح في runtime.json ← العنوان عربي/إنجليزي)
    private static final String[][] DATA_LABELS = {
            {"LastCalendarUnitName", "وحدة التقويم", "Calendar unit"},
            {"LastBudgetRequestNumber", "رقم طلب الميزانية", "Budget request no."},
            {"LastSupplementRequestNumber", "رقم طلب التعزيز", "Supplement request no."},
            {"LastTransferRequestNumber", "رقم طلب المناقلة", "Transfer request no."}};

    // JsonUtil بيكتب في اللوج "Saved key = value" لما يحفظ، فمنه بنعرف كل كيس عمل إيه على البيئة
    private static final Pattern SAVED = Pattern.compile("^Saved (\\w+) = (.*) in runtime\\.json$");

    public void onStart(ISuite suite) {
        SuiteStart = System.currentTimeMillis();
    }

    public void onFinish(ISuite suite) {
        List<ITestContext> Contexts = new ArrayList<>();
        for (ISuiteResult SuiteResult : suite.getResults().values()) Contexts.add(SuiteResult.getTestContext());
        Contexts.sort(Comparator.comparing(c -> c.getStartDate().getTime()));

        List<ITestResult> Tests = new ArrayList<>(), Setups = new ArrayList<>();
        for (ITestContext Context : Contexts) {
            Tests.addAll(Context.getPassedTests().getAllResults());
            Tests.addAll(Context.getFailedTests().getAllResults());
            Tests.addAll(Context.getSkippedTests().getAllResults());
            // فشل @BeforeMethod وغيره بيظهر كـ "فشل التجهيز" ومش بيتعد ضمن الكيسز
            Setups.addAll(Context.getFailedConfigurations().getAllResults());
        }
        // ممكن نفس الليسنر يتسجل من أكتر من Test Class، فنكتب التقرير لو فيه نتايج بس
        if (Tests.isEmpty()) return;
        Tests.sort(Comparator.comparingLong(ITestResult::getStartMillis));
        Setups.sort(Comparator.comparingLong(ITestResult::getStartMillis));

        List<ITestResult> All = new ArrayList<>(Tests);
        All.addAll(Setups);
        All.sort(Comparator.comparingLong(ITestResult::getStartMillis));
        long Start = Math.min(SuiteStart, All.get(0).getStartMillis());
        long End = All.stream().mapToLong(ITestResult::getEndMillis).max().orElse(System.currentTimeMillis());

        // ===== الوحدات =====
        Map<String, Module> Modules = new LinkedHashMap<>();
        Map<ITestResult, Module> ModuleOf = new LinkedHashMap<>();
        for (ITestResult R : All) {
            String[] Name = ModuleName(R, Tests);
            Module M = Modules.computeIfAbsent(Name[0], k -> new Module(Modules.size() + 1, Name[0], Name[1]));
            M.Start = Math.min(M.Start, R.getStartMillis());
            M.End = Math.max(M.End, R.getEndMillis());
            ModuleOf.put(R, M);
        }
        JsonArray ModulesJson = new JsonArray();
        for (Module M : Modules.values()) {
            JsonObject O = new JsonObject();
            O.addProperty("id", M.Id);
            O.addProperty("name", M.Name);
            O.addProperty("ar", M.Ar);
            O.addProperty("sec", Sec(M.End - M.Start));
            ModulesJson.add(O);
        }

        // ===== الكيسز (بترتيب التنفيذ) + بيانات الاختبار =====
        JsonArray TestsJson = new JsonArray(), DataJson = new JsonArray();
        int N = 0, SetupN = 900;
        for (ITestResult R : All) {
            boolean Setup = !R.getMethod().isTest();
            int Num = Setup ? ++SetupN : ++N;
            Module M = ModuleOf.get(R);
            JsonObject T = new JsonObject();
            T.addProperty("n", Num);
            T.addProperty("name", R.getMethod().getMethodName());
            if (!Description(R).isEmpty()) T.addProperty("ar", Description(R));
            T.addProperty("cls", R.getTestClass().getRealClass().getSimpleName());
            T.addProperty("mod", M.Id);
            T.addProperty("start", TIME.format(Instant.ofEpochMilli(R.getStartMillis())));
            T.addProperty("end", TIME.format(Instant.ofEpochMilli(R.getEndMillis())));
            T.addProperty("sec", Sec(R.getEndMillis() - R.getStartMillis()));
            T.addProperty("status", Setup ? "setup" : StatusKey(R.getStatus()));
            if (Setup || R.getStatus() == ITestResult.FAILURE) T.add("fail", Failure(R));
            TestsJson.add(T);

            if (Setup) continue;
            for (Map.Entry<String, String> Saved : SavedValues(R).entrySet()) {
                String[] Label = DataLabel(Saved.getKey());
                JsonObject D = new JsonObject();
                D.addProperty("mod", M.Name);
                D.addProperty("modAr", M.Ar);
                D.addProperty("n", Num);
                D.addProperty("test", R.getMethod().getMethodName());
                if (!Description(R).isEmpty()) D.addProperty("testAr", Description(R));
                D.addProperty("key", Saved.getKey());
                D.addProperty("ar", Label[0]);
                D.addProperty("en", Label[1]);
                D.addProperty("value", Saved.getValue());
                DataJson.add(D);
            }
        }

        // ===== بيانات الرن نفسه =====
        ZonedDateTime Day = Instant.ofEpochMilli(Start).atZone(CAIRO);
        JsonObject Date = new JsonObject();
        Date.addProperty("dow", Day.getDayOfWeek().getValue() % 7); // 0 = الأحد زي JavaScript
        Date.addProperty("d", Day.getDayOfMonth());
        Date.addProperty("m", Day.getMonthValue());
        Date.addProperty("y", Day.getYear());

        JsonObject Env = new JsonObject();
        Env.addProperty("browser", DriverFactoryClass.BrowserName == null ? "—"
                : Capitalize(DriverFactoryClass.BrowserName) + " " + DriverFactoryClass.BrowserVersion);
        Env.addProperty("selenium", new BuildInfo().getReleaseLabel());
        Env.addProperty("java", System.getProperty("java.version"));
        Env.addProperty("os", System.getProperty("os.name"));

        JsonObject Report = new JsonObject();
        Report.addProperty("suite", suite.getName());
        Report.add("date", Date);
        Report.addProperty("from", SHORT_TIME.format(Instant.ofEpochMilli(Start)));
        Report.addProperty("to", SHORT_TIME.format(Instant.ofEpochMilli(End)));
        Report.addProperty("fromS", TIME.format(Instant.ofEpochMilli(Start)));
        Report.addProperty("toS", TIME.format(Instant.ofEpochMilli(End)));
        Report.addProperty("totalSec", Sec(End - Start));
        Report.addProperty("retried", Tests.stream().filter(ITestResult::wasRetried).count());
        Report.add("env", Env);
        Report.add("modules", ModulesJson);
        Report.add("tests", TestsJson);
        Report.add("data", DataJson);

        // Gson بيهرّب < و > جوه النصوص (<)، فمفيش لوج أو رسالة خطأ تقدر تقفل الـ <script> بالغلط
        String Html = Resource("report/ResultReport.html")
                .replace("__SUITE__", Esc(suite.getName()))
                .replace("__NEC_LOGO__", "data:image/png;base64," + Base64.getEncoder().encodeToString(ResourceBytes("report/nec-logo.png")))
                .replace("__REPORT_DATA__", new Gson().toJson(Report));

        try {
            Path Dir = Paths.get(System.getProperty("user.dir"), "test-outputs");
            Files.createDirectories(Dir);
            Path File = Dir.resolve("Result-Report.html");
            Files.write(File, Html.getBytes(StandardCharsets.UTF_8));
            LogsUtiles.info("Result report written to " + File);
        } catch (IOException e) {
            LogsUtiles.error("Couldn't write result report: " + e.getMessage());
        }
    }

    // الوحدة: مرحلة الدورة لو الكيس من T1_BudgetCases، وإلا اسم الـ <test> في الـ xml
    // فشل التجهيز بياخد وحدة الكيس اللي كان بيجهّزله (أول كيس بعده)
    private static String[] ModuleName(ITestResult R, List<ITestResult> Tests) {
        ITestResult Owner = R;
        if (!R.getMethod().isTest()) {
            Owner = Tests.stream().filter(t -> t.getStartMillis() >= R.getStartMillis()).findFirst().orElse(Tests.get(Tests.size() - 1));
        }
        if (Owner.getTestClass().getName().equals(PHASED_CLASS)) {
            Object[] Phase = PHASES[0];
            for (Object[] P : PHASES) if (Owner.getMethod().getPriority() >= (int) P[0]) Phase = P;
            return new String[]{(String) Phase[1], (String) Phase[2]};
        }
        String Name = Owner.getTestContext().getName();
        return new String[]{Name, Name};
    }

    // تفاصيل الكيس الفاشل: الخطأ، السبب الجذري، اللوكيتور، السطر في الصفحة والتيست، الصورة، اللوج
    private static JsonObject Failure(ITestResult R) {
        JsonObject F = new JsonObject();
        Throwable Error = R.getThrowable();
        if (Error != null) {
            Throwable Root = Error;
            while (Root.getCause() != null && Root.getCause() != Root) Root = Root.getCause();
            String Cover = Covering(Root.getMessage());
            if (Cover == null) Cover = Covering(Error.getMessage());
            String Locator = Locator(Root.getMessage());
            if (Locator == null) Locator = Locator(Error.getMessage());

            F.addProperty("error", Error.getClass().getSimpleName());
            String[] Summary = ErrorSummary(Error);
            F.addProperty("msgAr", Summary[0]);
            F.addProperty("msgEn", Summary[1]);
            // مفيش سبب أعمق (زي Assertion من غير لوكيتور) ← التقرير بيقول "لا يوجد سبب جذري" بدل ما يكرر نفس الرسالة
            if (Root != Error || Cover != null || Locator != null) {
                F.addProperty("rootType", Root.getClass().getSimpleName());
                if (Cover != null) {
                    F.addProperty("noteKind", "cover");
                    F.addProperty("code", Cover);
                } else if (Locator != null) {
                    F.addProperty("noteKind", Root.getClass().getSimpleName().equals("NoSuchElementException") ? "locator" : "wait");
                    F.addProperty("code", Locator);
                } else {
                    F.addProperty("code", FirstLine(Root.getMessage()));
                }
            }

            StackTraceElement PageFrame = null, TestFrame = null;
            for (StackTraceElement Frame : Error.getStackTrace()) {
                String C = Frame.getClassName();
                if (TestFrame == null && C.startsWith("Tests.")) TestFrame = Frame;
                if (PageFrame == null && TestFrame == null && IsPageFrame(C)) PageFrame = Frame;
            }
            if (PageFrame != null) F.addProperty("point", PageFrame.toString());
            if (TestFrame != null) F.addProperty("line", TestFrame.toString());
            F.addProperty("stack", Utils.shortStackTrace(Error, false));
        }

        Object Shot = R.getAttribute("Screenshot");
        if (Shot != null) F.addProperty("shot", Shot.toString());

        JsonArray Log = new JsonArray();
        synchronized (Logs) {
            for (LogLine Line : Logs) {
                if (Line.Time < R.getStartMillis() || Line.Time > R.getEndMillis()) continue;
                JsonArray L = new JsonArray();
                L.add(TIME.format(Instant.ofEpochMilli(Line.Time)));
                L.add(Line.Message);
                Log.add(L);
            }
        }
        F.add("log", Log);
        return F;
    }

    // القيم اللي الكيس حفظها في runtime.json أثناء تشغيله
    private static Map<String, String> SavedValues(ITestResult R) {
        Map<String, String> Values = new LinkedHashMap<>();
        synchronized (Logs) {
            for (LogLine Line : Logs) {
                if (Line.Time < R.getStartMillis() || Line.Time > R.getEndMillis()) continue;
                Matcher M = SAVED.matcher(Line.Message);
                if (M.matches()) Values.put(M.group(1), M.group(2));
            }
        }
        return Values;
    }

    private static String[] DataLabel(String Key) {
        for (String[] L : DATA_LABELS) if (L[0].equals(Key)) return new String[]{L[1], L[2]};
        return new String[]{Key, Key};
    }

    private static String Description(ITestResult R) {
        String D = R.getMethod().getDescription();
        return D == null ? "" : D.trim();
    }

    // الصفحات هي أي كلاس بتاعنا مش Utility ولا Driver ولا Listener ولا Test (عشان نوصل لسطر الصفحة اللي فشل)
    private static boolean IsPageFrame(String ClassName) {
        for (String Prefix : new String[]{"java.", "javax.", "jdk.", "sun.", "org.", "com.", "io.", "net.",
                "Utilities.", "DriverFactory.", "Listeners.", "Base.", "Tests."})
            if (ClassName.startsWith(Prefix)) return false;
        return true;
    }

    // الوصف المختصر للخطأ: الـ Timeout بيتكتب بالثواني وفترة إعادة الفحص بدل رسالة Selenium الطويلة
    private static String[] ErrorSummary(Throwable Error) {
        String Message = Error.getMessage() == null ? "" : Error.getMessage();
        Matcher M = Pattern.compile("tried for (\\d+) second\\(s\\) with (\\d+) milliseconds? interval").matcher(Message);
        if (M.find())
            return new String[]{"انتهت مهلة الانتظار بعد " + M.group(1) + " ثانية (إعادة الفحص كل " + M.group(2) + " مللي ثانية)",
                    "Wait timed out after " + M.group(1) + " seconds (polling every " + M.group(2) + " ms)"};
        if (Error instanceof AssertionError)
            return new String[]{"فشل التحقق: " + FirstLine(Message), "Assertion failed: " + FirstLine(Message)};
        return new String[]{FirstLine(Message), FirstLine(Message)};
    }

    // بيطلع اللوكيتور من رسالة Selenium: إما {"selector":"..."} أو "located by By.xxx: ..."
    private static String Locator(String Message) {
        if (Message == null) return null;
        Matcher M = Pattern.compile("\"selector\":\"(.*?)\"\\}").matcher(Message);
        if (M.find()) return M.group(1).replace("\\\"", "\"");
        M = Pattern.compile("located by (By\\.[\\w.]+: .+?)(?: \\(tried for|\\n|$)").matcher(Message);
        if (M.find()) return M.group(1);
        return null;
    }

    // ElementClickIntercepted: العنصر اللي كان مغطي الزرار واستقبل الضغطة
    private static String Covering(String Message) {
        if (Message == null) return null;
        Matcher M = Pattern.compile("Other element would receive the click: (.+)").matcher(Message);
        return M.find() ? M.group(1).trim() : null;
    }

    private static String StatusKey(int Status) {
        return Status == ITestResult.SUCCESS ? "passed" : Status == ITestResult.FAILURE ? "failed" : "skipped";
    }

    private static double Sec(long Ms) {
        return Ms / 1000.0;
    }

    private static String Capitalize(String Text) {
        return Text.isEmpty() ? Text : Character.toUpperCase(Text.charAt(0)) + Text.substring(1);
    }

    private static String FirstLine(String Text) {
        return Text == null ? "" : Text.split("\\R", 2)[0];
    }

    private static String Esc(String Text) {
        if (Text == null) return "";
        return Text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;");
    }

    private static byte[] ResourceBytes(String Name) {
        try (InputStream In = ResultReportListener.class.getClassLoader().getResourceAsStream(Name)) {
            return In == null ? new byte[0] : In.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private static String Resource(String Name) {
        return new String(ResourceBytes(Name), StandardCharsets.UTF_8);
    }

    private static final class Module {
        final int Id;
        final String Name, Ar;
        long Start = Long.MAX_VALUE, End = 0;

        Module(int Id, String Name, String Ar) {
            this.Id = Id;
            this.Name = Name;
            this.Ar = Ar;
        }
    }

    private static final class LogLine {
        final long Time;
        final String Message;

        LogLine(long Time, String Message) {
            this.Time = Time;
            this.Message = Message;
        }
    }

    private static final class CaptureAppender extends AbstractAppender {
        CaptureAppender() {
            super("ResultReportCapture", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent Event) {
            synchronized (Logs) {
                Logs.add(new LogLine(Event.getTimeMillis(), Event.getMessage().getFormattedMessage()));
            }
        }
    }
}
