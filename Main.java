import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Main {
    private static final String BOT_TOKEN = "8937303127:AAFRz6GgLeFLAY1JeJyIcwLPXAhz_NutLcM";
    private static final String TELEGRAM_API = "https://api.telegram.org/bot" + BOT_TOKEN;

    public static void main(String[] args) {
        System.out.println("=== Бот успешно запущен и работает с кнопками! ===");
        long lastUpdateId = 0;

        while (true) {
            try {
                String urlString = TELEGRAM_API + "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=30";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    String json = response.toString();
                    
                    // Простая обработка обновлений
                    if (json.contains("\"update_id\":")) {
                        // Извлекаем update_id
                        int updateIdIndex = json.lastIndexOf("\"update_id\":");
                        int commaIndex = json.indexOf(",", updateIdIndex);
                        if (commaIndex != -1) {
                            String idStr = json.substring(updateIdIndex + 12, commaIndex).trim();
                            lastUpdateId = Long.parseLong(idStr);
                        }

                        // Проверяем нажатие на инлайн-кнопку (callback_query)
                        if (json.contains("\"callback_query\"")) {
                            long chatId = extractLong(json, "\"chat\":{\"id\":");
                            if (chatId == 0) {
                                chatId = extractLong(json, "\"id\":");
                            }
                            String data = extractString(json, "\"data\":\"");
                            
                            if (data != null) {
                                handleCommand(chatId, data);
                            }
                        } 
                        // Проверяем обычное текстовое сообщение
                        else if (json.contains("\"text\":")) {
                            long chatId = extractLong(json, "\"chat\":{\"id\":");
                            String text = extractString(json, "\"text\":\"");
                            
                            if (text != null) {
                                handleCommand(chatId, text);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Ошибка в цикле опроса: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private static void handleCommand(long chatId, String command) {
        switch (command.trim()) {
            case "/start":
                sendResponse(chatId, "Привет! Я твой круглосуточный Java-бот на Render. Используй кнопки ниже для управления:");
                break;
            case "/help":
                sendResponse(chatId, "Доступные команды:\n/start - Запустить бота\n/help - Помощь\n/roll - Бросить кубик (1-6)");
                break;
            case "/roll":
                int roll = new Random().nextInt(6) + 1;
                sendResponse(chatId, "🎲 Вам выпало: " + roll);
                break;
            default:
                sendResponse(chatId, "Эхо: " + command);
                break;
        }
    }

    private static void sendResponse(long chatId, String text) {
        try {
            String urlString = TELEGRAM_API + "/sendMessage";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            // Формируем JSON с текстом и красивыми кнопками (Inline Keyboard)
            String jsonInputString = "{"
                    + "\"chat_id\": " + chatId + ","
                    + "\"text\": \"" + escapeJson(text) + "\","
                    + "\"reply_markup\": {"
                    + "  \"inline_keyboard\": ["
                    + "    ["
                    + "      {\"text\": \"🚀 Старт\", \"callback_data\": \"/start\"},"
                    + "      {\"text\": \"ℹ️ Помощь\", \"callback_data\": \"/help\"}"
                    + "    ],"
                    + "    ["
                    + "      {\"text\": \"🎲 Бросить кубик\", \"callback_data\": \"/roll\"}"
                    + "    ]"
                    + "  ]"
                    + "}"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            conn.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static long extractLong(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return 0;
            int start = idx + key.length();
            int end = start;
            while (end < json.length() && Character.isDigit(json.charAt(end))) {
                end++;
            }
            return Long.parseLong(json.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String extractString(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return null;
            int start = idx + key.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return null;
            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private static String escapeJson(String text) {
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }
}

