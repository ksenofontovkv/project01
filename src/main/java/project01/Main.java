package project01;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {
    private static Logger logger = Logger.getLogger(Main.class.getName());
    private static final String DEFAULT_INPUT_FILE_LNG_TXT = "lng.txt";          //default input file
    private static final String DEFAULT_OUTPUT_FILE_LNG_OUT_TXT = "lng_out.txt"; //output file
    
    public static void main(String[] args) {
        File inputFile;
        if (args.length == 0) {
            inputFile = new File(DEFAULT_INPUT_FILE_LNG_TXT);
        } else {
            inputFile = new File(args[0]);
        }
        // Чтение файла:
        List<String> lines = new ArrayList<>(readInputFile(inputFile));
        // Расчёт групп (корзин) с номерами строк, некоторые корзины становятся пустыми, некоторые склеиваются:
        List<List<Integer>> buckets = groupLinesTogether(lines);
        // Обработка вывода результатов в файл групп (корзин):
        showResults(lines, buckets);
    }

    private static Set<String> readInputFile(final File f) {
        Set<String> lines = new LinkedHashSet<String>();
        try (final FileReader fileReader = new FileReader(f.getAbsolutePath())) {
            try (final BufferedReader br = new BufferedReader(fileReader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Файл не найден: " + f.getAbsolutePath());
            throw new RuntimeException("Ошибка при чтении файла: " + f.getAbsolutePath(), e);
        } catch (IOException e) {
            // we can ignore IOException when closing
            logger.log(Level.INFO, e.getMessage(), e);
        }
        return lines;
    }

    private static List<List<Integer>> groupLinesTogether(List<String> lines) {
        // Корзины с сортированными номерами строк, первоначально все строки в своей корзине, где только она
        List<List<Integer>> buckets = new ArrayList<>(lines.size());
        
        // Map: в какой корзине сейчас каждая строка
        int[] inWhichBucketLine = new int[lines.size()];
        
        // Map, где будут проверяться на совпадение значения внутри столбца: возьмем в среднем уникальных записей, как lines * 8
        Map<String, Integer> columnAndItsLineNumber = new HashMap<>(lines.size() * 8);
        
        // Обходим строки и столбцы
        int column;
        // matches a quoted field and an unquoted field
        String pureColumn = "(\s*\"([^\"]|\"\"[^\"])*\"\s*|\s*([^\";]*)\s*)";
        Pattern stringOkPattern = Pattern.compile("^(" + pureColumn + ";)*" + pureColumn + "$");
        int row = 1;
        for (final String line : lines) {
            if (!stringOkPattern.matcher(line).matches()) {
                logger.log(Level.SEVERE, "Error parsing line, ignoring: " + line);
                buckets.add(Collections.emptyList());
                row++;
                continue;
            }
            buckets.add(new ArrayList<Integer>(Collections.singleton(row)));
            inWhichBucketLine[row - 1] = row;

            column = 0;
            String[] columns = line.split(";");
            for (String value : columns) {
                column++;
                value = value.trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                    value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
                }
                if (value.isEmpty() || value.equals("\"\"")) {
                    continue;
                }

                mergeBucketsIfFound(buckets, inWhichBucketLine, columnAndItsLineNumber, value, column, row);
            }
            row++;
        }
        return buckets;
    }

    private static void mergeBucketsIfFound(List<List<Integer>> buckets, int[] inWhichBucketLine,
            Map<String, Integer> columnAndItsLineNumber, String value, final int column, final int row) {
        String columnValueKey = column + "-" + value;

        if (!columnAndItsLineNumber.containsKey(columnValueKey)) {
            columnAndItsLineNumber.put(columnValueKey, row);
            return;
        }

        Integer index = columnAndItsLineNumber.get(columnValueKey);
        Integer bucket = inWhichBucketLine[row - 1];
        Integer existentBucket = inWhichBucketLine[index - 1];
        if (!bucket.equals(existentBucket)) {
            Integer inBacket = Math.min(bucket, existentBucket);
            Integer outBacket = Math.max(bucket, existentBucket);
            List<Integer> outRows = buckets.get(outBacket - 1);
            List<Integer> inRowsBacket = buckets.get(inBacket - 1);
            for (Integer backetChild : outRows) {
                inRowsBacket.add(backetChild);
                inWhichBucketLine[backetChild - 1] = inBacket;
            }
            buckets.set(outBacket - 1, Collections.emptyList());
        }
    }

    private static void showResults(final List<String> lines, final List<List<Integer>> buckets) {
        List<List<Integer>> backetsSortedByLength = buckets
                .stream()
                .sorted((c1, c2) -> Integer.valueOf(c2.size()).compareTo(c1.size())) // reverse order
                .collect(Collectors.toList());
        
        // Считаем группы размером > 1
        long multiElementGroupsCount = backetsSortedByLength.stream().filter((e) -> e.size() > 1).count();

        // Запись в файл
        String outputPath = DEFAULT_OUTPUT_FILE_LNG_OUT_TXT;
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputPath)))) {
            writer.println("Количество групп с более чем одним элементом: " + multiElementGroupsCount);
            writer.println();

            int groupNumber = 1;
            for (List<Integer> group : backetsSortedByLength) {
                if (group.size() == 0) {
                    break;
                }
                writer.println("Группа " + groupNumber++);
                for (Integer line : group) {
                    writer.println(lines.get(line - 1));
                }
                writer.println();
            }
        } catch (IOException e) {
            // we can ignore IOException when closing
            logger.log(Level.INFO, e.getMessage(), e);
        }
    }

}
