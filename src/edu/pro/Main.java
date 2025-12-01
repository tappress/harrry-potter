package edu.pro;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
 * Оптимізована версія для мінімального споживання пам'яті.
 *
 * Оптимізації:
 * - Потокове читання файлу (BufferedReader) замість завантаження всього файлу
 * - StringTokenizer замість String.split() для економії пам'яті
 * - PriorityQueue для топ-K замість сортування всієї колекції
 * - Примітивний int замість Long для підрахунку частоти
 * - Обробка рядок за рядком без створення проміжних масивів
 */
public class Main {

    private static final String DEFAULT_FILE_PATH = "src/edu/pro/txt/harry.txt";
    private static final int TOP_WORDS_COUNT = 30;

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
            final Map<String, int[]> wordFrequencies = calculateWordFrequenciesStreaming(filePath);
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
     * Читає файл потоково та підраховує частоту слів.
     * Оптимізація: не завантажує весь файл в пам'ять.
     *
     * @param filePath шлях до файлу
     * @return Map зі словами та їх частотою
     * @throws IOException якщо виникла помилка читання файлу
     */
    public static Map<String, int[]> calculateWordFrequenciesStreaming(String filePath) throws IOException {
        final Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("Файл не знайдено: " + filePath);
        }

        final Map<String, int[]> frequencies = new HashMap<>(10000);
        final StringBuilder wordBuffer = new StringBuilder(50);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.ISO_8859_1))) {
            int ch;
            while ((ch = reader.read()) != -1) {
                if (isLetter(ch)) {
                    wordBuffer.append(Character.toLowerCase((char) ch));
                } else if (wordBuffer.length() > 0) {
                    processWord(wordBuffer.toString(), frequencies);
                    wordBuffer.setLength(0);
                }
            }

            if (wordBuffer.length() > 0) {
                processWord(wordBuffer.toString(), frequencies);
            }
        }

        return frequencies;
    }

    /**
     * Перевіряє чи символ є літерою (a-z, A-Z).
     */
    private static boolean isLetter(int ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    /**
     * Додає слово до Map частот.
     * Використовує int[] для уникнення boxing/unboxing.
     */
    private static void processWord(String word, Map<String, int[]> frequencies) {
        int[] count = frequencies.get(word);
        if (count == null) {
            frequencies.put(word, new int[]{1});
        } else {
            count[0]++;
        }
    }

    /**
     * Виводить топ N найчастіших слів.
     * Оптимізація: використовує PriorityQueue замість сортування всієї колекції.
     *
     * @param wordFrequencies Map зі словами та їх частотою
     * @param count           кількість слів для виведення
     */
    public static void printTopWords(Map<String, int[]> wordFrequencies, int count) {
        PriorityQueue<Map.Entry<String, int[]>> minHeap = new PriorityQueue<>(
                count + 1,
                Comparator.comparingInt(e -> e.getValue()[0])
        );

        for (Map.Entry<String, int[]> entry : wordFrequencies.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > count) {
                minHeap.poll();
            }
        }

        String[] results = new String[minHeap.size()];
        int i = results.length - 1;
        while (!minHeap.isEmpty()) {
            Map.Entry<String, int[]> entry = minHeap.poll();
            results[i--] = entry.getKey() + " " + entry.getValue()[0];
        }

        for (String result : results) {
            System.out.println(result);
        }
    }
}
