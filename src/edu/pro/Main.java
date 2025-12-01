package edu.pro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Програма для аналізу частоти слів у текстовому файлі.
 * Читає текст, очищує його від спеціальних символів,
 * підраховує частоту кожного слова та виводить топ найчастіших слів.
 */
public class Main {

    private static final String DEFAULT_FILE_PATH = "src/edu/pro/txt/harry.txt";
    private static final int TOP_WORDS_COUNT = 30;
    private static final Pattern NON_LETTER_PATTERN = Pattern.compile("[^a-zA-Z\\s]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * Точка входу в програму.
     *
     * @param args аргументи командного рядка (опціонально: шлях до файлу)
     */
    public static void main(String[] args) {
        final LocalDateTime start = LocalDateTime.now();

        final String filePath = args.length > 0 ? args[0] : DEFAULT_FILE_PATH;

        try {
            final String cleanedText = readAndCleanText(filePath);
            final Map<String, Long> wordFrequencies = calculateWordFrequencies(cleanedText);
            printTopWords(wordFrequencies, TOP_WORDS_COUNT);
        } catch (IOException e) {
            System.err.println("Помилка читання файлу: " + e.getMessage());
            return;
        }

        final LocalDateTime finish = LocalDateTime.now();
        System.out.println("------");
        System.out.println("Час виконання: " + ChronoUnit.MILLIS.between(start, finish) + " мс");
    }

    /**
     * Читає текст з файлу та очищує його від спеціальних символів.
     *
     * @param filePath шлях до файлу
     * @return очищений текст у нижньому регістрі
     * @throws IOException якщо виникла помилка читання файлу
     */
    public static String readAndCleanText(String filePath) throws IOException {
        final Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("Файл не знайдено: " + filePath);
        }

        final String content = new String(Files.readAllBytes(path));
        return NON_LETTER_PATTERN.matcher(content)
                .replaceAll(" ")
                .toLowerCase();
    }

    /**
     * Підраховує частоту кожного слова в тексті.
     *
     * @param text очищений текст
     * @return Map зі словами та їх частотою, відсортована за спаданням частоти
     */
    public static Map<String, Long> calculateWordFrequencies(String text) {
        return Arrays.stream(WHITESPACE_PATTERN.split(text))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Виводить топ N найчастіших слів.
     *
     * @param wordFrequencies Map зі словами та їх частотою
     * @param count           кількість слів для виведення
     */
    public static void printTopWords(Map<String, Long> wordFrequencies, int count) {
        wordFrequencies.entrySet()
                .stream()
                .limit(count)
                .forEach(entry -> System.out.println(entry.getKey() + " " + entry.getValue()));
    }
}