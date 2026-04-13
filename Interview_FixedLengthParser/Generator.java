import java.io.*;
import java.util.*;

public class Generator {

    private static final String SCHEMA_PATH = "Interview_FixedLengthParser/schema.txt";

    public static String fixedLengthParserGenerator() {
        StringBuilder sb = new StringBuilder();

        try {
            List<Triple> columns = getSchema();

            appendImports(sb);
            sb.append("public class FixedLengthParser {\n\n");

            appendSchemaConstants(sb, columns);
            appendParseMethod(sb, columns);
            appendExtractMethod(sb);
            appendMainMethod(sb);

            sb.append("}\n");

        } catch (Exception e) {
            throw new RuntimeException("Error generating parser", e);
        }

        return sb.toString();
    }

    private static void appendImports(StringBuilder sb) {
        sb.append("""
                import java.io.BufferedReader;
                import java.io.FileReader;
                import java.io.IOException;
                import java.util.ArrayList;
                import java.util.List;

                """);
    }

    private static void appendSchemaConstants(StringBuilder sb, List<Triple> columns) {
        sb.append("    // Schema configuration\n");

        for (Triple col : columns) {
            String name = col.name.toUpperCase();
            sb.append(String.format("    private static final int %s_START = %d;%n", name, col.start));
            sb.append(String.format("    private static final int %s_END = %d;%n", name, col.end));
        }
        sb.append("\n");
    }

    private static void appendParseMethod(StringBuilder sb, List<Triple> columns) {
        int maxEnd = columns.stream().mapToInt(c -> c.end).max().orElse(0);

        sb.append("""
                public List<Record> parseFile(String filePath) throws IOException {
                    List<Record> records = new ArrayList<>();

                    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                """);

        sb.append(String.format("""
                            if (line.length() < %d) {
                                continue;
                            }

                """, maxEnd));

        // Generate field extraction dynamically
        for (Triple col : columns) {
            sb.append(String.format(
                    "            String %s = extractField(line, %s_START, %s_END).trim();%n",
                    col.name, col.name.toUpperCase(), col.name.toUpperCase()
            ));
        }

        // Constructor
        sb.append("\n            Record record = new Record(");
        for (Iterator<Triple> it = columns.iterator(); it.hasNext(); ) {
            sb.append(it.next().name);
            if (it.hasNext()) sb.append(", ");
        }
        sb.append(");\n");

        sb.append("""
                            records.add(record);
                        }
                    }
                    return records;
                }

                """);
    }

    private static void appendExtractMethod(StringBuilder sb) {
        sb.append("""
                private String extractField(String line, int start, int end) {
                    return line.substring(start - 1, Math.min(end, line.length()));
                }

                """);
    }

    private static void appendMainMethod(StringBuilder sb) {
        sb.append("""
                public static void main(String[] args) {
                    FixedLengthParser parser = new FixedLengthParser();
                    try {
                        List<Record> records = parser.parseFile("path/to/file.txt");
                        records.forEach(System.out::println);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                """);
    }

    public static List<Triple> getSchema() throws IOException {
        List<Triple> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(SCHEMA_PATH))) {
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
        }

        return list;
    }

    public static String recordGenerator() {
        StringBuilder sb = new StringBuilder();

        try {
            List<Triple> columns = getSchema();

            sb.append("public class Record {\n\n");

            // Fields
            for (Triple col : columns) {
                sb.append("    private String ").append(col.name).append(";\n");
            }

            // Constructor
            sb.append("\n    public Record(");
            for (Iterator<Triple> it = columns.iterator(); it.hasNext(); ) {
                Triple col = it.next();
                sb.append("String ").append(col.name);
                if (it.hasNext()) sb.append(", ");
            }
            sb.append(") {\n");

            for (Triple col : columns) {
                sb.append(String.format("        this.%s = %s;%n", col.name, col.name));
            }
            sb.append("    }\n");

            // toString
            sb.append("""
                    
                    @Override
                    public String toString() {
                        return "Record{"
                    """);

            for (Iterator<Triple> it = columns.iterator(); it.hasNext(); ) {
                Triple col = it.next();
                sb.append(String.format(" + \"%s='\" + %s + '\\''", col.name, col.name));
                if (it.hasNext()) sb.append(" + \",\"");
            }

            sb.append(" + '}';\n    }\n");

            sb.append("}\n");

        } catch (IOException e) {
            throw new RuntimeException("Error generating Record class", e);
        }

        return sb.toString();
    }

    public static void main(String[] args) {
//        System.out.println(fixedLengthParserGenerator());
         System.out.println(recordGenerator());
    }
}