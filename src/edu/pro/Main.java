package edu.pro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Програма для аналізу частоти слів у текстовому файлі.
 * Оптимізована версія для максимальної швидкодії.
 *
 * Оптимізації швидкодії:
 * - Читання всього файлу в пам'ять одним викликом (Files.readAllBytes)
 * - Lookup таблиці для O(1) перевірки літер та toLowerCase
 * - Пряма робота з байтами без регулярних виразів
 * - Попередньо виділений HashMap з оптимальним розміром
 * - Мінімізація boxing/unboxing через int[]
 * - PriorityQueue для топ-K за O(n log k)
 * - Уникнення String.split() та регулярних виразів
 */
public class Main {

    private static final String DEFAULT_FILE_PATH = "src/edu/pro/txt/harry.txt";
    private static final int TOP_WORDS_COUNT = 30;

    // Lookup таблиця для швидкої перевірки чи є символ літерою (O(1) замість умов)
    private static final boolean[] IS_LETTER = new boolean[256];

    // Таблиця для швидкого перетворення в lowercase (O(1) замість умов)
    private static final byte[] TO_LOWER = new byte[256];

    static {
        for (int i = 0; i < 256; i++) {
            IS_LETTER[i] = (i >= 'a' && i <= 'z') || (i >= 'A' && i <= 'Z');
            TO_LOWER[i] = (byte) ((i >= 'A' && i <= 'Z') ? i + 32 : i);
        }
    }

    /**
     * Точка входу в програму.
     *
     * @param args аргументи командного рядка (опціонально: шлях до файлу)
     */
    public static void main(String[] args) {
        final LocalDateTime start = LocalDateTime.now();

        final Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        final long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        final String filePath = args.length > 0 ? args[0] : DEFAULT_FILE_PATH;

        try {
            final Map<String, int[]> wordFrequencies = calculateWordFrequenciesFast(filePath);
            printTopWords(wordFrequencies, TOP_WORDS_COUNT);
        } catch (IOException e) {
            System.err.println("Помилка читання файлу: " + e.getMessage());
            return;
        }

        final LocalDateTime finish = LocalDateTime.now();

        runtime.gc();
        final long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        System.out.println("------");
        System.out.println("Час виконання: " + ChronoUnit.MILLIS.between(start, finish) + " мс");
        System.out.println("Використано пам'яті: " + (memoryAfter - memoryBefore) / 1024 + " KB");
    }

    /**
     * Читає файл та підраховує частоту слів з максимальною швидкістю.
     *
     * @param filePath шлях до файлу
     * @return Map зі словами та їх частотою
     * @throws IOException якщо виникла помилка читання файлу
     */
    public static Map<String, int[]> calculateWordFrequenciesFast(String filePath) throws IOException {
        final Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("Файл не знайдено: " + filePath);
        }

        // Читання всього файлу в пам'ять - найшвидший спосіб для файлів < 10 MB
        final byte[] data = Files.readAllBytes(path);
        final int length = data.length;

        // HashMap з початковою ємністю для уникнення rehashing
        final Map<String, int[]> frequencies = new HashMap<>(15000);

        // Буфер для побудови слова
        final byte[] wordBuffer = new byte[100];
        int wordLength = 0;

        // Один прохід по всіх байтах
        for (int i = 0; i < length; i++) {
            final int b = data[i] & 0xFF;

            if (IS_LETTER[b]) {
                wordBuffer[wordLength++] = TO_LOWER[b];
            } else if (wordLength > 0) {
                // Створюємо String та додаємо до Map
                final String word = new String(wordBuffer, 0, wordLength);
                final int[] count = frequencies.get(word);
                if (count == null) {
                    frequencies.put(word, new int[]{1});
                } else {
                    count[0]++;
                }
                wordLength = 0;
            }
        }

        // Останнє слово
        if (wordLength > 0) {
            final String word = new String(wordBuffer, 0, wordLength);
            final int[] count = frequencies.get(word);
            if (count == null) {
                frequencies.put(word, new int[]{1});
            } else {
                count[0]++;
            }
        }

        return frequencies;
    }

    /**
     * Виводить топ N найчастіших слів.
     * Використовує min-heap для O(n log k) замість повного сортування O(n log n).
     *
     * @param wordFrequencies Map зі словами та їх частотою
     * @param count           кількість слів для виведення
     */
    public static void printTopWords(Map<String, int[]> wordFrequencies, int count) {
        final PriorityQueue<Map.Entry<String, int[]>> minHeap = new PriorityQueue<>(
                count + 1,
                Comparator.comparingInt(e -> e.getValue()[0])
        );

        for (Map.Entry<String, int[]> entry : wordFrequencies.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > count) {
                minHeap.poll();
            }
        }

        final String[] results = new String[minHeap.size()];
        int i = results.length - 1;
        while (!minHeap.isEmpty()) {
            final Map.Entry<String, int[]> entry = minHeap.poll();
            results[i--] = entry.getKey() + " " + entry.getValue()[0];
        }

        for (String result : results) {
            System.out.println(result);
        }
    }
}
