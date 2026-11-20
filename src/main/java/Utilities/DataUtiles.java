package Utilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class DataUtiles {
    public final static String TEST_DATA_PATH =
            System.getProperty("user.dir") + "/src/test/TestData/";

    // الباسوردات ممكن تتبعت من أمر التشغيل (-DAD_Password=...) أو من الـ CI كـ env (TESTDATA_AD_Password)
    // فبتتقدم على اللي في الجيسون، والـ prefix عشان مفتاح زي ID ميتلخبطش مع env variable تاني في الجهاز
    // ولو المفتاح مش موجود بنرمي Exception باسمه بدل ما نرجع "" والكيس يفشل في مكان بعيد عن السبب
    public static String getJsonData(String jsonFilename, String field) {
        String Override = System.getProperty(field, System.getenv("TESTDATA_" + field));
        if (Override != null) return Override;
        try (FileReader reader = new FileReader(TEST_DATA_PATH + jsonFilename + ".json")) {
            JsonElement Value = JsonParser.parseReader(reader).getAsJsonObject().get(field);
            if (Value == null) throw new IllegalArgumentException("Key '" + field + "' not found in " + jsonFilename + ".json");
            return Value.getAsString();
        } catch (IOException e) {
            throw new RuntimeException("Can't read " + jsonFilename + ".json", e);
        }
    }

    public static String getPropertyValue(String fileName, String key) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream Stream = new FileInputStream(TEST_DATA_PATH + fileName + ".properties")) {
            properties.load(Stream);
        }
        return properties.getProperty(key);
    }

    // نفس اللي فوق بس بقيمة افتراضية، للإعدادات الاختيارية (لو المفتاح مش موجود منوقفش الرن)
    public static String getPropertyValue(String fileName, String key, String defaultValue) {
        try {
            String Value = getPropertyValue(fileName, key);
            return Value == null ? defaultValue : Value.trim();
        } catch (IOException e) {
            return defaultValue;
        }
    }

        // نفس الـ run كله هياخد نفس الـ suffix
        private static final String RUN_ID = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        // بيرجع القيمة من الجيسون + suffix فريد
        public static String getUniqueJsonData (String fileName, String key) {
            String baseValue = getJsonData(fileName, key);
            return baseValue + "_" + RUN_ID;
        }

        // بيرجع القيمة من الجيسون + الساعة بالدقايق عشان الاسم ميتكررش
        public static String getJsonDataWithTime (String fileName, String key) {
            String baseValue = getJsonData(fileName, key);
            return baseValue + " " + new SimpleDateFormat("HH:mm").format(new Date());
        }

        // بيرجع القيمة من الجيسون + اليوم والساعة والدقايق أرقام بس من غير رموز ولا مسافات (مثال: 990020099152231)
        public static String getJsonDataWithDayAndTime (String fileName, String key) {
            String baseValue = getJsonData(fileName, key);
            return baseValue + new SimpleDateFormat("ddHHmm").format(new Date());
        }

        // بيرجع القيمة من الجيسون + اليوم والشهر والساعة والدقيقة (مثال: وحدة اوتوميشن 19112222) عشان الاسم ميتكررش بين الرنات
        public static String getJsonDataWithDateAndTime (String fileName, String key) {
            String baseValue = getJsonData(fileName, key);
            return baseValue + " " + new SimpleDateFormat("ddMMHHmm").format(new Date());
        }
    }
