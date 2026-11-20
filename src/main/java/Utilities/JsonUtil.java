package Utilities;

import com.google.gson.*;
import java.io.*;

// الداتا الديناميك (آخر فعالية/طلب/يوزر) بتتكتب في runtime.json مش Data.json
// عشان الداتا الأساسية متتعدلش كل رن، وبتتقرا بـ DataUtiles.getJsonData("runtime", key)
public class JsonUtil {

    private static final String FILE_PATH =
            System.getProperty("user.dir") +
                    "/src/test/TestData/runtime.json";

    // synchronized عشان لو الرن بقى parallel اتنين ميكتبوش في نفس الوقت ويبوظوا الملف
    public static synchronized void writeValue(String key, String value) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject json;

            File file = new File(FILE_PATH);
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }
            } else {
                json = new JsonObject();
            }

            json.addProperty(key, value);

            try (FileWriter writer = new FileWriter(FILE_PATH)) {
                gson.toJson(json, writer);
            }
            LogsUtiles.info("Saved " + key + " = " + value + " in runtime.json");

        } catch (Exception e) {
            throw new RuntimeException("Can't write " + key + " in runtime.json", e);
        }
    }

    public static synchronized String readValue(String key) {
        try (FileReader reader = new FileReader(FILE_PATH)) {
            JsonElement Value = JsonParser.parseReader(reader).getAsJsonObject().get(key);
            if (Value == null) return null;
            // التقرير بيقرا السطر ده عشان يكتب تحت الكيس القيمة اللي اشتغل عليها
            LogsUtiles.info("Read " + key + " = " + Value.getAsString() + " from runtime.json");
            return Value.getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}
