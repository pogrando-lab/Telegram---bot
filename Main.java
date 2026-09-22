import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Main {
    private static final String BOT_TOKEN = "8070966952:AAE320BpHv64S_L5Z80S5rR02hC3XG9M0YQ";
    private static final String TELEGRAM_API = "https://api.telegram.org/bot" + BOT_TOKEN;

    public static void main(String[] args) {
        System.out.println("=== Бот успешно запущен и работает 24/7! ===");
        long lastUpdateId = 0;

        while (true) {
            try {
                String urlString = TELEGRAM_API + "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=30";
                String response = sendApiRequest(urlString, "GET", null);

                if (response != null && response.contains("\"ok\":true")) {
                    int index = 0;
                    while ((index = response.indexOf("\"update_id\":", index)) != -1) {
                        index += "\"update_id\":".length();
                        int commaIndex = response.indexOf(",", index);
                        if (commaIndex != -1) {
                            try {
                                long updateId = Long.parseLong(response.substring(index, commaIndex).trim());
                                if (updateId > lastUpdateId) {
                                    lastUpdateId = updateId;
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    if (response.contains("\"text\":")) {
                        processMessages(response);
                    }
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println("Ошибка в цикле опроса: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private static void processMessages(String json) {
        int msgIndex = 0;
        while ((msgIndex = json.indexOf("\"message\":", msgIndex)) != -1) {
            int chatIndex = json.indexOf("\"chat\":{", msgIndex);
            if (chatIndex == -1) break;

            int idIndex = json.indexOf("\"id\":", chatIndex);
            if (idIndex == -1) break;
            int idEnd = json.indexOf(",", idIndex);
            if (idEnd == -1) break;
            String chatId = json.substring(idIndex + 5, idEnd).trim();

            int textIndex = json.indexOf("\"text\":\"", msgIndex);
            if (textIndex != -1) {
                textIndex += 8;
                int textEnd = json.indexOf("\"", textIndex);
                if (textEnd != -1) {
                    String text = json.substring(textIndex, textEnd);
                    handleCommand(chatId, text);
                }
            }
            msgIndex = chatIndex + 1;
        }
    }

    private static void handleCommand(String chatId, String text) {
        System.out.println("Получено сообщение от " + chatId + ": " + text);
        String replyText;

        if (text.equals("/start")) {
            replyText = "Привет! Я твой круглосуточный Java-бот на Render. Напиши /help для списка команд.";
        } else if (text.equals("/help")) {
            replyText = "Доступные команды:\n/start - Запустить бота\n/help - Помощь\n/roll - Бросить кубик (1-6)";
        } else if (text.equals("/roll")) {
            int roll = new Random().nextInt(6) + 1;
            replyText = "🎲 Вам выпало: " + roll;
        } else {
            replyText = "Эхо: " + text;
        }

        sendMessage(chatId, replyText);
    }

    private static void sendMessage(String chatId, String text) {
        try {
            String urlString = TELEGRAM_API + "/sendMessage";
            String payload = "chat_id=" + chatId + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
            sendApiRequest(urlString, "POST", payload);
        } catch (Exception e) {
            System.out.println("Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    private static String sendApiRequest(String urlString, String method, String payload) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(35000);

        if (payload != null && method.equals("POST")) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}
