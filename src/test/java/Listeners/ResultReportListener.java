package Listeners;

import DriverFactory.DriverFactoryClass;
import Utilities.LogsUtiles;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// بعد ما الرن يخلص بيكتب test-outputs/Result-Report.html بنفس شكل تقرير "نتيجة الاختبار"
// (عربي/إنجليزي + Dark mode + تفاصيل كل كيس فاشل: الخطأ، السبب الجذري، اللوكيتور، السطر في الصفحة والتيست، الصورة، اللوج)
// الـ CSS والـ JS ثابتين في src/main/resources/report، والجافا بتبني الداتا بس
public class ResultReportListener implements ISuiteListener {

    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(CAIRO);
    private static final DateTimeFormatter SHORT_TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(CAIRO);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(CAIRO);
    private static final String CHECK = "<svg viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M3 8.5l3.2 3.2L13 5\" /></svg>";
    private static final String CROSS = "<svg viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M4 4l8 8M12 4l-8 8\" /></svg>";

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

    public void onStart(ISuite suite) {
        SuiteStart = System.currentTimeMillis();
    }

    public void onFinish(ISuite suite) {
        // كل <test> في الـ xml = Module، مترتبين بوقت بدايتهم
        Map<String, List<ITestResult>> Modules = new LinkedHashMap<>();
        List<ITestContext> Contexts = new ArrayList<>();
        for (ISuiteResult SuiteResult : suite.getResults().values()) Contexts.add(SuiteResult.getTestContext());
        Contexts.sort(Comparator.comparing(c -> c.getStartDate().getTime()));
        List<ITestResult> All = new ArrayList<>();
        for (ITestContext Context : Contexts) {
            List<ITestResult> Results = new ArrayList<>();
            Results.addAll(Context.getPassedTests().getAllResults());
            Results.addAll(Context.getFailedTests().getAllResults());
            Results.addAll(Context.getSkippedTests().getAllResults());
            if (Results.isEmpty()) continue;
            Results.sort(Comparator.comparingLong(ITestResult::getStartMillis));
            Modules.put(Context.getName(), Results);
            All.addAll(Results);
        }
        // ممكن نفس الليسنر يتسجل من أكتر من Test Class، فنكتب التقرير لو فيه نتايج بس
        if (All.isEmpty()) return;
        All.sort(Comparator.comparingLong(ITestResult::getStartMillis));

        int Total = All.size();
        int Passed = Count(All, ITestResult.SUCCESS), Failed = Count(All, ITestResult.FAILURE), Skipped = Count(All, ITestResult.SKIP);
        int Retried = (int) All.stream().filter(ITestResult::wasRetried).count();
        long End = All.stream().mapToLong(ITestResult::getEndMillis).max().orElse(System.currentTimeMillis());
        long Start = Math.min(SuiteStart, All.get(0).getStartMillis());
        long Duration = End - Start;
        long MaxDur = Math.max(1, All.stream().mapToLong(r -> r.getEndMillis() - r.getStartMillis()).max().orElse(1));

        StringBuilder H = new StringBuilder();
        H.append("<!DOCTYPE html>\n<html lang=\"ar\" dir=\"rtl\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n");
        H.append("<title>نتيجة الاختبار</title><style>").append(Resource("report/ResultReport.css")).append("</style>");
        H.append("<script>try{if(localStorage.getItem('result-report-lang')==='en'){document.documentElement.lang='en';document.documentElement.dir='ltr'}}catch(e){}</script></head><body>\n");
        H.append("<div class=\"wrap\">\n");

        // ===== الهيدر =====
        String From = SHORT_TIME.format(Instant.ofEpochMilli(Start)), To = SHORT_TIME.format(Instant.ofEpochMilli(End));
        String Day = DATE.format(Instant.ofEpochMilli(Start));
        H.append("<header class=\"top\"><div><h1>").append(Bi("نتيجة الاختبار", "Test Result")).append("</h1>\n");
        H.append("<div class=\"meta\">").append(Bi(Ltr(suite.getName()) + " · " + Ltr(Day) + " · من " + Ltr(From) + " إلى " + Ltr(To) + " (توقيت القاهرة)",
                Ltr(suite.getName()) + " · " + Ltr(Day) + " · " + Ltr(From) + " to " + Ltr(To) + " (Cairo time)")).append("</div>");
        H.append("<div class=\"chips\">");
        if (DriverFactoryClass.BrowserName != null)
            H.append(Chip(Capitalize(DriverFactoryClass.BrowserName), DriverFactoryClass.BrowserVersion));
        H.append(Chip("Selenium", new BuildInfo().getReleaseLabel()));
        H.append(Chip("Java", System.getProperty("java.version")));
        H.append("<span class=\"chip\"><b>").append(Bi("نظام التشغيل", "OS")).append("</b> ")
                .append(Ltr(System.getProperty("os.name") + " " + System.getProperty("os.arch"))).append("</span>");
        H.append("</div></div>\n");
        boolean Success = Failed == 0;
        H.append("<div class=\"topr\"><span class=\"status ").append(Success ? "good\">" + CHECK : "bad\">" + CROSS.replace(" aria-hidden=\"true\"", ""))
                .append(Success ? Bi("نجاح التنفيذ", "BUILD SUCCESS") : Bi("فشل التنفيذ", "BUILD FAILURE")).append("</span>");
        H.append("<div class=\"btns\"><button id=\"lang\" class=\"btn\" type=\"button\">").append(Bi("English", "العربية"))
                .append("</button><button id=\"theme\" class=\"btn\" type=\"button\">◐ ").append(Bi("المظهر", "Theme")).append("</button></div></div></header>\n\n");

        // ===== بيانات الدورة =====
        H.append(RunData(All));

        // ===== الأرقام (KPIs) =====
        H.append("<section class=\"kpis\">");
        H.append(Kpi("", Bi("إجمالي الاختبارات", "Total tests"), String.valueOf(Total),
                Bi("عدد الوحدات: " + Modules.size(), "Modules: " + Modules.size())));
        H.append(Kpi(Passed > 0 ? "good" : "", Bi("ناجح", "Passed"), String.valueOf(Passed),
                Bi(Ltr(Percent(Passed, Total)) + " من الإجمالي", Ltr(Percent(Passed, Total)) + " of total")));
        H.append(Kpi(Failed > 0 ? "bad" : "", Bi("فاشل", "Failed"), String.valueOf(Failed),
                Bi(Ltr(Percent(Failed, Total)) + " من الإجمالي", Ltr(Percent(Failed, Total)) + " of total")));
        H.append(Kpi("", Bi("متخطى / معاد", "Skipped / Retried"), Bi(Skipped + " / " + Retried, Ltr(Skipped + " / " + Retried)),
                Skipped == 0 ? Bi("لا توجد اختبارات متخطاة", "No skipped tests") : Bi("عدد المتخطاة: " + Skipped, "Skipped: " + Skipped)));
        H.append(Kpi("", Bi("نسبة النجاح", "Pass rate"), Ltr(Percent(Passed, Total)),
                Bi(Passed + " من " + Total, Passed + " of " + Total)));
        H.append(Kpi("", Bi("مدة التنفيذ", "Duration"), DurBi(Duration),
                Bi(String.format(Locale.ENGLISH, "%,d", Duration) + " مللي ثانية", Ltr(String.format(Locale.ENGLISH, "%,d ms", Duration)))));
        H.append("</section>\n\n");

        // ===== توزيع النتائج (Donut) + النتائج حسب الوحدة =====
        H.append("<section class=\"grid2\">\n<div class=\"card\"><h2>").append(Bi("توزيع النتائج", "Results distribution")).append("</h2><div class=\"dwrap\">");
        H.append(Donut(Passed, Failed, Skipped, Total));
        H.append("<ul class=\"legend\">")
                .append("<li><i class=\"sw good\"></i>").append(Bi("ناجح", "Passed")).append(" <b>").append(Ltr(String.valueOf(Passed))).append("</b></li>")
                .append("<li><i class=\"sw bad\"></i>").append(Bi("فاشل", "Failed")).append(" <b>").append(Ltr(String.valueOf(Failed))).append("</b></li>")
                .append("<li><i class=\"sw skip\"></i>").append(Bi("متخطى", "Skipped")).append(" <b>").append(Ltr(String.valueOf(Skipped))).append("</b></li>")
                .append("</ul></div></div>\n");

        int MaxModule = Modules.values().stream().mapToInt(List::size).max().orElse(1);
        H.append("<div class=\"card\"><h2>").append(Bi("النتائج حسب الوحدة", "Results by module")).append("</h2><div class=\"mgrid\"><div class=\"mhead\">")
                .append("<span>").append(Bi("الوحدة", "Module")).append("</span><span></span><span>").append(Bi("ناجح", "Passed"))
                .append("</span><span>").append(Bi("المدة", "Duration")).append("</span><span>").append(Bi("الحالة", "Status")).append("</span></div>");
        for (Map.Entry<String, List<ITestResult>> Module : Modules.entrySet()) {
            List<ITestResult> R = Module.getValue();
            String Name = Module.getKey();
            int MP = Count(R, ITestResult.SUCCESS), MF = Count(R, ITestResult.FAILURE), MS = Count(R, ITestResult.SKIP);
            long MDur = R.get(R.size() - 1).getEndMillis() - R.get(0).getStartMillis();
            H.append("<div class=\"mrow\"><div class=\"mname\">").append(Ltr(Name)).append("</div><div class=\"mbar\"><div class=\"mtrack\">");
            H.append(Seg("good", MP, R.size(), MaxModule, Name, "ناجح", "Passed"));
            H.append(Seg("bad", MF, R.size(), MaxModule, Name, "فاشل", "Failed"));
            H.append(Seg("skip", MS, R.size(), MaxModule, Name, "متخطى", "Skipped"));
            H.append("</div></div>\n<div class=\"mnum\">").append(Bi(MP + " من " + R.size(), Ltr(MP + "/" + R.size()))).append("</div>");
            H.append("<div class=\"mdur\">").append(DurBi(MDur)).append("</div><div class=\"mst\">").append(Pill(MF > 0 ? ITestResult.FAILURE : ITestResult.SUCCESS)).append("</div></div>");
        }
        H.append("</div></div>\n</section>\n");

        // ===== الاختبارات بترتيب التنفيذ =====
        H.append("<section class=\"card\"><div class=\"thead\"><h2>").append(Bi("الاختبارات بترتيب التنفيذ (" + Total + ")", "Tests in run order (" + Total + ")")).append("</h2>\n");
        H.append("<div class=\"ctl\"><div class=\"chipsf\" role=\"group\" aria-label=\"تصفية حسب الحالة\" data-attr=\"aria-label\" data-ar=\"تصفية حسب الحالة\" data-en=\"Filter by status\">");
        H.append("<button class=\"fc on\" data-f=\"all\" type=\"button\">").append(Bi("الكل", "All")).append(" <b>").append(Total).append("</b></button>");
        H.append("<button class=\"fc\" data-f=\"passed\" type=\"button\">").append(Bi("ناجح", "Passed")).append(" <b>").append(Passed).append("</b></button>");
        H.append("<button class=\"fc\" data-f=\"failed\" type=\"button\">").append(Bi("فاشل", "Failed")).append(" <b>").append(Failed).append("</b></button>");
        if (Skipped > 0) H.append("<button class=\"fc\" data-f=\"skipped\" type=\"button\">").append(Bi("متخطى", "Skipped")).append(" <b>").append(Skipped).append("</b></button>");
        H.append("</div>\n<input id=\"q\" type=\"search\" placeholder=\"ابحث باسم الاختبار أو الوحدة…\" data-attr=\"placeholder\" data-ar=\"ابحث باسم الاختبار أو الوحدة…\" data-en=\"Search by test or module name…\"></div></div>\n");
        H.append("<div class=\"tscroll\"><table id=\"tt\"><thead><tr><th data-s=\"idx\" class=\"n\">#</th><th data-s=\"name\">").append(Bi("الاختبار", "Test"))
                .append("</th><th data-s=\"mod\">").append(Bi("الوحدة", "Module")).append("</th><th data-s=\"idx\">").append(Bi("البداية", "Start"))
                .append("</th><th data-s=\"dur\">").append(Bi("المدة", "Duration")).append("</th><th data-s=\"status\">").append(Bi("الحالة", "Status"))
                .append("</th></tr></thead><tbody>");
        int Idx = 0;
        for (ITestResult R : All) {
            Idx++;
            String Module = R.getTestContext().getName();
            String Method = R.getMethod().getMethodName();
            long Dur = R.getEndMillis() - R.getStartMillis();
            boolean IsFailed = R.getStatus() == ITestResult.FAILURE;
            // البحث بيلاقي الكيس باسمه العربي أو بالقيمة اللي تحته (رقم الطلب مثلاً)
            String Search = (Method + " " + Description(R) + " " + String.join(" ", CaseValues(R).values())).toLowerCase();
            H.append("<tr data-status=\"").append(StatusKey(R.getStatus())).append("\" data-idx=\"").append(Idx).append("\" data-name=\"").append(Esc(Search))
                    .append("\" data-dur=\"").append(Dur).append("\" data-mod=\"").append(Esc(Module)).append("\"")
                    .append(IsFailed ? " class=\"xp\" tabindex=\"0\" aria-expanded=\"false\">" : ">").append("\n");
            H.append("<td class=\"n\">").append(IsFailed ? "<span class=\"chev\" aria-hidden=\"true\"></span>" : "").append(Idx).append("</td>");
            H.append("<td><div class=\"tn\">").append(CaseName(R)).append("</div>").append(CaseData(R)).append("</td>\n");
            H.append("<td>").append(Ltr(Esc(Module))).append("</td><td class=\"tm\">").append(Ltr(TIME.format(Instant.ofEpochMilli(R.getStartMillis())))).append("</td>\n");
            H.append("<td class=\"dcell\"><span class=\"dbar\"><i class=\"").append(IsFailed ? "bad" : "").append("\" style=\"width:").append(Pct(Dur, MaxDur))
                    .append("%\"></i></span><span class=\"dv\">").append(DurBi(Dur)).append("</span></td>\n");
            H.append("<td>").append(Pill(R.getStatus())).append("</td></tr>");
            if (IsFailed) H.append(FailureDetails(R));
        }
        H.append("</tbody></table></div>\n<div class=\"count\" id=\"cnt\"></div></section>\n");

        // ===== أبطأ الاختبارات =====
        List<ITestResult> Slowest = new ArrayList<>(All);
        Slowest.sort(Comparator.comparingLong((ITestResult r) -> r.getEndMillis() - r.getStartMillis()).reversed());
        Slowest = Slowest.subList(0, Math.min(8, Slowest.size()));
        long SlowMax = Math.max(1, Slowest.get(0).getEndMillis() - Slowest.get(0).getStartMillis());
        H.append("<section class=\"card\"><h2>").append(Bi("أبطأ " + Slowest.size() + " اختبارات", "Slowest " + Slowest.size() + " tests")).append("</h2><div class=\"slow\">");
        for (ITestResult R : Slowest) {
            long Dur = R.getEndMillis() - R.getStartMillis();
            String Name = Esc(R.getMethod().getMethodName()), Module = Esc(R.getTestContext().getName());
            String[] D = Dur(Dur);
            H.append("<div class=\"srow\"><div class=\"sname\">")
                    .append(R.getStatus() == ITestResult.FAILURE ? "<svg class=\"x\" viewBox=\"0 0 16 16\" aria-label=\"Failed\"><path d=\"M4 4l8 8M12 4l-8 8\" /></svg>" : "")
                    .append(CaseName(R)).append("</div>\n<div class=\"sbar\"><i style=\"width:").append(Pct(Dur, SlowMax)).append("%\" tabindex=\"0\" data-t=\"").append(Name)
                    .append("\" data-d=\"").append(D[0]).append(" · ").append(Module).append("\" data-de=\"").append(D[1]).append(" · ").append(Module)
                    .append("\"></i></div><div class=\"sval\">").append(DurBi(Dur)).append("</div></div>");
        }
        H.append("</div></section>\n\n");

        H.append("<footer>").append(Bi("أُنشئ تلقائيًا بواسطة " + Ltr("TestNG") + " عند نهاية التنفيذ · نتيجة الاختبار",
                "Generated automatically by TestNG at the end of the run · Test Result")).append("</footer>\n");
        H.append("</div><div id=\"tip\" role=\"tooltip\"></div><script>").append(Resource("report/ResultReport.js")).append("</script></body></html>");

        try {
            Path Dir = Paths.get(System.getProperty("user.dir"), "test-outputs");
            Files.createDirectories(Dir);
            Path Report = Dir.resolve("Result-Report.html");
            Files.write(Report, H.toString().getBytes(StandardCharsets.UTF_8));
            LogsUtiles.info("Result report written to " + Report);
        } catch (IOException e) {
            LogsUtiles.error("Couldn't write result report: " + e.getMessage());
        }
    }

    // القيم اللي بتظهر في كارت بيانات الدورة بالترتيب (المفتاح في runtime.json ← العنوان عربي/إنجليزي)
    private static final String[][] RUN_DATA = {
            {"LastCalendarUnitName", "وحدة التقويم", "Calendar unit"},
            {"LastBudgetRequestNumber", "رقم طلب الميزانية", "Budget request no."},
            {"LastSupplementRequestNumber", "رقم طلب التعزيز", "Supplement request no."}};

    // JsonUtil بيكتب في اللوج "Saved key = value" لما يحفظ و"Read key = value" لما يقرا، فمنهم بنعرف كل كيس عمل/استخدم إيه
    private static final Pattern SAVED = Pattern.compile("^Saved (\\w+) = (.*) in runtime\\.json$");
    private static final Pattern READ = Pattern.compile("^Read (\\w+) = (.*) from runtime\\.json$");

    private static Map<String, String> ValuesInLog(ITestResult R, Pattern P) {
        Map<String, String> Values = new LinkedHashMap<>();
        synchronized (Logs) {
            for (LogLine Line : Logs) {
                if (Line.Time < R.getStartMillis() || Line.Time > R.getEndMillis()) continue;
                Matcher M = P.matcher(Line.Message);
                if (M.matches()) Values.put(M.group(1), M.group(2));
            }
        }
        return Values;
    }

    // لو الكيس عمل قيمة بنعرضها هي بس، ولو معملش بنعرض اللي اشتغل عليها
    private static Map<String, String> CaseValues(ITestResult R) {
        Map<String, String> Created = ValuesInLog(R, SAVED);
        return Created.isEmpty() ? ValuesInLog(R, READ) : Created;
    }

    private static String CaseData(ITestResult R) {
        Map<String, String> Values = CaseValues(R);
        if (Values.isEmpty()) return "";
        boolean Created = !ValuesInLog(R, SAVED).isEmpty();
        List<String> Parts = new ArrayList<>();
        for (String Value : Values.values()) Parts.add(DataValue(Value));
        return "<div class=\"tc\">" + (Created ? Bi("أنشأ", "Created") : Bi("استخدم", "Used")) + " " + String.join(" · ", Parts) + "</div>";
    }

    // أرقام الطلبات (EBE-... / EBS-...) بخط ثابت العرض، والأسماء العربي بالخط العادي
    private static String DataValue(String Value) {
        return Value.matches("[A-Z]+-[0-9-]+") ? "<span class=\"ltr mono\">" + Esc(Value) + "</span>" : "<bdi>" + Esc(Value) + "</bdi>";
    }

    // اسم الكيس بالعربي من @Test(description) ولو مش موجود بيظهر اسم الميثود في الاتنين
    private static String Description(ITestResult R) {
        String D = R.getMethod().getDescription();
        return D == null ? "" : D.trim();
    }

    private static String CaseName(ITestResult R) {
        String Method = Ltr(Esc(R.getMethod().getMethodName()));
        String Ar = Description(R);
        return Ar.isEmpty() ? Method : Bi(Esc(Ar), Method);
    }

    // كارت بيانات الدورة: القيم اللي اتعملت في الرن ده بس، عشان منعرضش قيمة قديمة من runtime.json
    private static String RunData(List<ITestResult> All) {
        Map<String, String> Created = new LinkedHashMap<>();
        for (ITestResult R : All) Created.putAll(ValuesInLog(R, SAVED));
        StringBuilder S = new StringBuilder("<section class=\"card rdata\"><div class=\"rhead\"><h2>").append(Bi("بيانات الدورة", "Run data"))
                .append("</h2><span class=\"rsub\">").append(Bi("القيم التي تم إنشاؤها على البيئة في هذه الدورة", "Values created on the environment in this run"))
                .append("</span></div><div class=\"rgrid\">");
        for (String[] Item : RUN_DATA) {
            String Value = Created.get(Item[0]);
            boolean Mono = Value != null && Value.matches("[A-Z]+-[0-9-]+");
            S.append("<div class=\"ritem").append(Value == null ? " none" : "").append("\">");
            if (Value != null) S.append("<button class=\"rcp\" type=\"button\" data-v=\"").append(Esc(Value)).append("\">").append(Bi("نسخ", "Copy")).append("</button>");
            S.append("<div class=\"rl\">").append(Bi(Item[1], Item[2])).append("</div><div class=\"rv").append(Mono ? " mono" : "").append("\">")
                    .append(Value == null ? Bi("لم يتم إنشاؤه في هذه الدورة", "Not created in this run") : Esc(Value)).append("</div></div>");
        }
        return S.append("</div></section>\n").toString();
    }

    // تفاصيل الكيس الفاشل: بتظهر لما تدوس على السطر
    private static String FailureDetails(ITestResult R) {
        Throwable Error = R.getThrowable();
        StringBuilder X = new StringBuilder("<tr class=\"xrow\" style=\"display:none\"><td colspan=\"6\"><div class=\"xbox\">");
        if (Error != null) {
            Throwable Root = Error;
            while (Root.getCause() != null && Root.getCause() != Root) Root = Root.getCause();
            String Locator = Locator(Root.getMessage());
            if (Locator == null) Locator = Locator(Error.getMessage());

            X.append("<div class=\"xgrid\"><div><div class=\"xk\">").append(Bi("الخطأ", "Error")).append("</div><div class=\"v\">")
                    .append(Ltr(Error.getClass().getSimpleName())).append("</div><div class=\"sm\">").append(ErrorSummary(Error)).append("</div></div>");
            X.append("<div><div class=\"xk\">").append(Bi("السبب الجذري", "Root cause")).append("</div><div class=\"v\">").append(Ltr(Root.getClass().getSimpleName())).append("</div>");
            if (Locator != null) {
                boolean NotFound = Root.getClass().getSimpleName().equals("NoSuchElementException");
                X.append("<div class=\"sm\">").append(NotFound ? Bi("محدد العنصر الذي لم يُعثر عليه", "Locator that found no element")
                        : Bi("محدد العنصر الذي كان الاختبار ينتظره", "Locator the test was waiting for")).append("</div><code class=\"loc\">").append(Esc(Locator)).append("</code>");
            } else if (Root == Error) {
                // مفيش سبب أعمق (زي Assertion)، فمنكررش نفس رسالة الخطأ
                X.append("<div class=\"sm\">").append(Bi("لا يوجد سبب أعمق — الخطأ نفسه هو السبب", "No deeper cause — the error itself is the cause")).append("</div>");
            } else {
                X.append("<div class=\"sm\">").append(Ltr(Esc(FirstLine(Root.getMessage())))).append("</div>");
            }
            X.append("</div></div>");

            StackTraceElement PageFrame = null, TestFrame = null;
            for (StackTraceElement Frame : Error.getStackTrace()) {
                String C = Frame.getClassName();
                if (TestFrame == null && C.startsWith("Tests.")) TestFrame = Frame;
                if (PageFrame == null && TestFrame == null && IsPageFrame(C)) PageFrame = Frame;
            }
            X.append("<div class=\"xgrid\"><div><div class=\"xk\">").append(Bi("موضع الفشل في كود الصفحة", "Failure point (Page Object)")).append("</div><code>")
                    .append(PageFrame == null ? "—" : Esc(PageFrame.toString())).append("</code></div>");
            X.append("<div><div class=\"xk\">").append(Bi("السطر في الاختبار", "Line in the test")).append("</div><code>")
                    .append(TestFrame == null ? "—" : Esc(TestFrame.toString())).append("</code></div></div>");
        }

        Object Shot = R.getAttribute("Screenshot");
        if (Shot != null) {
            X.append("<a class=\"shotlink\" href=\"#\" target=\"_blank\" rel=\"noopener\" data-png=\"").append(Shot)
                    .append("\"><svg viewBox=\"0 0 16 16\" aria-hidden=\"true\"><rect x=\"1.5\" y=\"3.5\" width=\"13\" height=\"10\" rx=\"2\"/><circle cx=\"8\" cy=\"8.5\" r=\"2.6\"/><path d=\"M5.5 3.5l1-2h3l1 2\"/></svg>")
                    .append(Bi("صورة الصفحة وقت الفشل", "Failure screenshot")).append(" ↗</a>");
        }

        List<LogLine> Lines = new ArrayList<>();
        synchronized (Logs) {
            for (LogLine Line : Logs) if (Line.Time >= R.getStartMillis() && Line.Time <= R.getEndMillis()) Lines.add(Line);
        }
        if (!Lines.isEmpty()) {
            X.append("<div class=\"xk\">").append(Bi("سجل التنفيذ (" + Lines.size() + " سطر)", "Execution log (" + Lines.size() + " lines)")).append("</div><div class=\"logs\" dir=\"ltr\">");
            for (int i = 0; i < Lines.size(); i++) {
                LogLine Line = Lines.get(i);
                X.append("<div class=\"ll lv-").append(Line.Level).append(i == Lines.size() - 1 ? " last" : "").append("\"><span class=\"lt\">")
                        .append(TIME.format(Instant.ofEpochMilli(Line.Time))).append("</span><span class=\"ls\">").append(Esc(Line.Source))
                        .append("</span><span class=\"lm\">").append(Esc(Line.Message)).append("</span></div>");
            }
            X.append("</div>");
        }

        if (Error != null) {
            X.append("<details><summary>").append(Bi("تتبع الخطأ الكامل", "Full stack trace")).append("</summary><button class=\"copy\" type=\"button\">")
                    .append(Bi("نسخ", "Copy")).append("</button><pre>").append(Esc(Utils.shortStackTrace(Error, false))).append("</pre></details>");
        }
        X.append("</div></td></tr>\n");
        return X.toString();
    }

    // الصفحات هي أي كلاس بتاعنا مش Utility ولا Driver ولا Listener ولا Test (عشان نوصل لسطر الصفحة اللي فشل)
    private static boolean IsPageFrame(String ClassName) {
        for (String Prefix : new String[]{"java.", "javax.", "jdk.", "sun.", "org.", "com.", "io.", "net.",
                "Utilities.", "DriverFactory.", "Listeners.", "Base.", "Tests."})
            if (ClassName.startsWith(Prefix)) return false;
        return true;
    }

    // الوصف المختصر للخطأ: الـ Timeout بيتكتب بالثواني وفترة إعادة الفحص بدل رسالة Selenium الطويلة
    private static String ErrorSummary(Throwable Error) {
        String Message = Error.getMessage() == null ? "" : Error.getMessage();
        Matcher M = Pattern.compile("tried for (\\d+) second\\(s\\) with (\\d+) milliseconds? interval").matcher(Message);
        if (M.find())
            return Bi("انتهت مهلة الانتظار بعد " + M.group(1) + " ثانية (إعادة الفحص كل " + M.group(2) + " مللي ثانية)",
                    "Wait timed out after " + M.group(1) + " seconds (polling every " + M.group(2) + " ms)");
        if (Error instanceof AssertionError)
            return Bi("فشل التحقق: ", "Assertion failed: ") + Ltr(Esc(FirstLine(Message)));
        return Ltr(Esc(FirstLine(Message)));
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

    private static String Donut(int Passed, int Failed, int Skipped, int Total) {
        double C = 2 * Math.PI * 64;
        int[] Values = {Passed, Failed, Skipped};
        String[] Strokes = {"var(--good)", "var(--bad)", "var(--skip)"};
        String[][] Labels = {{"ناجح", "Passed"}, {"فاشل", "Failed"}, {"متخطى", "Skipped"}};
        long Parts = java.util.Arrays.stream(Values).filter(v -> v > 0).count();
        double Gap = Parts > 1 ? 3 : 0;
        StringBuilder S = new StringBuilder("<svg viewBox=\"0 0 180 180\" class=\"donut\" role=\"img\" aria-label=\"Passed " + Passed + " / " + Total + "\">\n");
        S.append("<g transform=\"rotate(-90 90 90)\" fill=\"none\" stroke-width=\"22\">\n<circle cx=\"90\" cy=\"90\" r=\"64\" stroke=\"var(--track)\" />\n");
        double Offset = 0;
        for (int i = 0; i < 3; i++) {
            if (Values[i] == 0) continue;
            double Len = C * Values[i] / Total;
            S.append(String.format(Locale.ENGLISH, "<circle cx=\"90\" cy=\"90\" r=\"64\" stroke=\"%s\" stroke-dasharray=\"%.2f %.2f\" stroke-dashoffset=\"-%.2f\" tabindex=\"0\" data-t=\"%s\" data-d=\"العدد: %d · %s\" data-te=\"%s\" data-de=\"Count: %d · %s\"/>\n",
                    Strokes[i], Math.max(0, Len - Gap), C, Offset, Labels[i][0], Values[i], Percent(Values[i], Total), Labels[i][1], Values[i], Percent(Values[i], Total)));
            Offset += Len;
        }
        S.append("</g>\n<text x=\"90\" y=\"88\" text-anchor=\"middle\" class=\"d-num\">").append(Percent(Passed, Total)).append("</text>\n");
        S.append("<text x=\"90\" y=\"108\" text-anchor=\"middle\" class=\"d-lbl t-ar\">نسبة النجاح</text><text x=\"90\" y=\"108\" text-anchor=\"middle\" class=\"d-lbl t-en\">Pass rate</text></svg>\n");
        return S.toString();
    }

    private static String Seg(String Cls, int Count, int ModuleSize, int MaxModule, String Module, String Ar, String En) {
        if (Count == 0) return "";
        return "<i class=\"seg " + Cls + "\" style=\"width:" + Pct(Count, MaxModule) + "%\" tabindex=\"0\" data-t=\"" + Esc(Module) + " · " + Ar
                + "\" data-d=\"" + Count + " من " + ModuleSize + "\" data-te=\"" + Esc(Module) + " · " + En + "\" data-de=\"" + Count + " of " + ModuleSize + "\"></i>";
    }

    private static String Pill(int Status) {
        if (Status == ITestResult.SUCCESS) return "<span class=\"pill good\">" + CHECK + Bi("ناجح", "Passed") + "</span>";
        if (Status == ITestResult.FAILURE) return "<span class=\"pill bad\">" + CROSS + Bi("فاشل", "Failed") + "</span>";
        return "<span class=\"pill skip\">" + Bi("متخطى", "Skipped") + "</span>";
    }

    private static String StatusKey(int Status) {
        return Status == ITestResult.SUCCESS ? "passed" : Status == ITestResult.FAILURE ? "failed" : "skipped";
    }

    private static String Kpi(String Cls, String Label, String Value, String Sub) {
        return "<div class=\"kpi " + Cls + "\"><div class=\"kl\">" + Label + "</div><div class=\"kv\">" + Value + "</div><div class=\"ks\">" + Sub + "</div></div>";
    }

    private static String Chip(String Name, String Value) {
        return "<span class=\"chip\"><b>" + Esc(Name) + "</b> " + Ltr(Esc(Value)) + "</span>";
    }

    // المدة بالشكل اللي في التقرير: 2.4 ث / 1 د 01 ث / 1 س 05 د
    private static String[] Dur(long Ms) {
        if (Ms < 60_000) {
            String S = String.format(Locale.ENGLISH, "%.1f", Ms / 1000.0);
            return new String[]{S + " ث", S + "s"};
        }
        long Sec = Ms / 1000;
        if (Sec < 3600) return new String[]{String.format(Locale.ENGLISH, "%d د %02d ث", Sec / 60, Sec % 60), String.format(Locale.ENGLISH, "%dm %02ds", Sec / 60, Sec % 60)};
        return new String[]{String.format(Locale.ENGLISH, "%d س %02d د", Sec / 3600, Sec % 3600 / 60), String.format(Locale.ENGLISH, "%dh %02dm", Sec / 3600, Sec % 3600 / 60)};
    }

    private static String DurBi(long Ms) {
        String[] D = Dur(Ms);
        return Bi(D[0], Ltr(D[1]));
    }

    private static int Count(List<ITestResult> Results, int Status) {
        return (int) Results.stream().filter(r -> r.getStatus() == Status).count();
    }

    private static String Percent(int Part, int Total) {
        return Total == 0 ? "0%" : String.format(Locale.ENGLISH, "%.1f%%", Part * 100.0 / Total);
    }

    private static String Pct(long Part, long Max) {
        return String.format(Locale.ENGLISH, "%.2f", Math.min(100.0, Part * 100.0 / Max));
    }

    private static String Bi(String Ar, String En) {
        return "<span class=\"t-ar\">" + Ar + "</span><span class=\"t-en\">" + En + "</span>";
    }

    private static String Ltr(String Text) {
        return "<span class=\"ltr\">" + Text + "</span>";
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

    private static String Resource(String Name) {
        try (InputStream In = ResultReportListener.class.getClassLoader().getResourceAsStream(Name)) {
            return In == null ? "" : new String(In.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static final class LogLine {
        final long Time;
        final String Level, Source, Message;

        LogLine(long Time, String Level, String Source, String Message) {
            this.Time = Time;
            this.Level = Level;
            this.Source = Source;
            this.Message = Message;
        }
    }

    // LogsUtiles بيسمّي الـ logger بالـ stack frame (Package.Class.method(File.java:10))
    // فبنحوّله لـ Class.method:10 زي ما بيظهر في التقرير
    private static final Pattern FRAME = Pattern.compile("^(?:[\\w$]+\\.)*([\\w$]+)\\.([\\w$<>]+)\\([^:()]*:(\\d+)\\)$");

    private static final class CaptureAppender extends AbstractAppender {
        CaptureAppender() {
            super("ResultReportCapture", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent Event) {
            String Name = Event.getLoggerName();
            Matcher M = FRAME.matcher(Name == null ? "" : Name);
            String Source = M.matches() ? M.group(1) + "." + M.group(2) + ":" + M.group(3) : Name;
            synchronized (Logs) {
                Logs.add(new LogLine(Event.getTimeMillis(), Event.getLevel().name().toLowerCase(Locale.ROOT), Source,
                        Event.getMessage().getFormattedMessage()));
            }
        }
    }
}
