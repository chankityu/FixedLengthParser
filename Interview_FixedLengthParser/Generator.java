import java.io.*;
import java.util.*;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

public class Generator {

    private static final String SCHEMA_PATH = "Interview_FixedLengthParser/schema.txt";

    private static TemplateEngine getTemplateEngine() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode("TEXT");
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    public static String fixedLengthParserGenerator() {
        try {
            List<Triple> columns = getSchema(SCHEMA_PATH);

            Context context = new Context();
            context.setVariable("columns", columns);
            context.setVariable("maxEnd",
                    columns.stream().mapToInt(c -> c.end).max().orElse(0));

            return getTemplateEngine().process(getParserTemplate(), context);

        } catch (Exception e) {
            throw new RuntimeException("Error generating parser", e);
        }
    }

    public static String recordGenerator() {
        try {
            List<Triple> columns = getSchema(SCHEMA_PATH);

            Context context = new Context();
            context.setVariable("columns", columns);

            return getTemplateEngine().process(getRecordTemplate(), context);

        } catch (Exception e) {
            throw new RuntimeException("Error generating Record class", e);
        }
    }

    // ================= TEMPLATE: PARSER =================
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
                String [[${col.name}]] = extractField(line, [[${#strings.toUpperCase(col.name)}]]_START, [[${#strings.toUpperCase(col.name)}]]_END).trim();
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
    }

    // ================= TEMPLATE: RECORD =================
    private static String getRecordTemplate() {
        return """
public class Record {

[# th:each="col : ${columns}"]
    private String [[${col.name}]];
[/]

    public Record(
[# th:each="col, iter : ${columns}"]
        String [[${col.name}]][(${iter.last}? '' : ',')]
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

    // ================= SCHEMA =================
    public static List<Triple> getSchema(String path) throws IOException {
        List<Triple> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");

                if (parts.length != 3) {
                    throw new IllegalArgumentException("Invalid schema line: " + line);
                }

                list.add(new Triple(
                        parts[0],
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                ));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid schema line");
        }

        return list;
    }
}