import java.io.*;
import java.util.*;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

public class Generator {

    private final TemplateEngine templateEngine;

    public Generator(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    // ===== PURE GENERATION =====
    public String generateParser(List<Triple> columns) {
        Context context = new Context();
        context.setVariable("columns", columns);
        context.setVariable("maxEnd",
                columns.stream().mapToInt(c -> c.getEnd()).max().orElse(0));

        return templateEngine.process(getParserTemplate(), context);
    }

    public String generateRecord(List<Triple> columns) {
        Context context = new Context();
        context.setVariable("columns", columns);

        return templateEngine.process(getRecordTemplate(), context);
    }

    // ===== ORCHESTRATION =====
    public String generateParserFromFile(String path) throws IOException {
        return generateParser(getSchema(path));
    }

    public String generateRecordFromFile(String path) throws IOException {
        return generateRecord(getSchema(path));
    }

    // ===== SCHEMA =====
    public static List<String> readSchemaLines(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.lines().collect(java.util.stream.Collectors.toList());
        }
    }

    public static List<Triple> parseSchema(List<String> lines) {
        List<Triple> list = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");

            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid schema line: " + line);
            }

            try {
                list.add(new Triple(
                        parts[0],
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                ));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid schema line: " + line);
            }
        }

        return list;
    }

    public static List<Triple> getSchema(String path) throws IOException {
        return parseSchema(readSchemaLines(path));
    }

    // ===== TEMPLATES =====
    private static String getParserTemplate() {
        return """
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FixedLengthParser {

    // Schema configuration
    [# th:each="col : ${columns}"]
    private static final int [[${#strings.toUpperCase(col.name)}]]_START = [[${col.start}]];
    private static final int [[${#strings.toUpperCase(col.name)}]]_END = [[${col.end}]];
    [/]

    public List<Record> parseFile(String filePath) throws IOException {
        List<Record> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.length() < [[${maxEnd}]]) {
                    continue;
                }

                [# th:each="col : ${columns}"]
                String [[${col.name}]] = extractField(
                        line,
                        [[${#strings.toUpperCase(col.name)}]]_START,
                        [[${#strings.toUpperCase(col.name)}]]_END
                ).trim();
                [/]

                Record record = new Record(
                    [# th:each="col, iter : ${columns}"]
                    [[${col.name}]][(${iter.last}? '' : ', ')]
                    [/]
                );

                records.add(record);
            }
        }

        return records;
    }

    private String extractField(String line, int start, int end) {
        return line.substring(start - 1, Math.min(end, line.length()));
    }

    public static void main(String[] args) {
        FixedLengthParser parser = new FixedLengthParser();

        try {
            List<Record> records = parser.parseFile("path/to/file.txt");
            records.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
""";
    } // keep your template

    // ================= TEMPLATE: RECORD =================
    private static String getRecordTemplate() {
        return """
public class Record {

    [# th:each="col : ${columns}"]
    private String [[${col.name}]];
    [/]

    public Record(
        [# th:each="col, iter : ${columns}"]
        String [[${col.name}]][(${iter.last}? '' : ', ')]
        [/]
    ) {

        [# th:each="col : ${columns}"]
        this.[[${col.name}]] = [[${col.name}]];
        [/]
    }

    @Override
    public String toString() {
        return "Record{" +
            [# th:each="col, iter : ${columns}"]
            "[[${col.name}]]='" + [[${col.name}]] + '\\'' +
            [(${iter.last}? '' : '", " +')]
            [/]
            '}';
    }
}
""";
    }
}