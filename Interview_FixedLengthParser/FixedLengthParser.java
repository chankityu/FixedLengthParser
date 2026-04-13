import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FixedLengthParser {

    // Schema configuration
    private static final int NAME_START = 1;
    private static final int NAME_END = 20;
    private static final int GENDER_START = 20;
    private static final int GENDER_END = 21;
    private static final int AGE_START = 22;
    private static final int AGE_END = 25;

    public List<Record> parseFile(String filePath) throws IOException {
        List<Record> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() < AGE_END) {
                    // Handle lines that are shorter than expected
                    continue;
                }

                // Extract fields based on fixed positions
                String name = extractField(line, NAME_START, NAME_END).trim();
                String gender = extractField(line, GENDER_START, GENDER_END).trim();
                String age = extractField(line, AGE_START, AGE_END).trim();

                // Create a new Record object
                Record record = new Record(name, gender, age);
                records.add(record);
            }
        }
        return records;
    }

    private String extractField(String line, int start, int end) {
        return line.substring(start - 1, end);
    }

    public static void main(String[] args) {
        FixedLengthParser parser = new FixedLengthParser();
        try {
            List<Record> records = parser.parseFile("path/to/your/file.txt");
            for (Record record : records) {
                System.out.println(record);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
