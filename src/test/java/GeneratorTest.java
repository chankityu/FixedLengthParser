import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {

    private static final String TEST_SCHEMA_PATH = "Interview_FixedLengthParser/schema.txt";

    @BeforeEach
    void setup() throws IOException {
        Files.createDirectories(Paths.get("Interview_FixedLengthParser"));

        String schema = """
                name 1 10
                age 11 13
                city 14 25
                """;

        Files.writeString(Paths.get(TEST_SCHEMA_PATH), schema);
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(TEST_SCHEMA_PATH));
    }

    @Test
    void testGetSchema_validSchema() throws Exception {
        List<Triple> schema = Generator.getSchema(TEST_SCHEMA_PATH);

        assertEquals(3, schema.size());

        assertEquals("name", schema.get(0).name);
        assertEquals(1, schema.get(0).start);
        assertEquals(10, schema.get(0).end);
    }

    @Test
    void testGetSchema_invalidSchemaLine() throws IOException {
        String invalidSchema = "invalid line here";
        Files.writeString(Paths.get(TEST_SCHEMA_PATH), invalidSchema);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> Generator.getSchema(TEST_SCHEMA_PATH));
        System.out.println(ex.getMessage());
        assertTrue(ex.getMessage().contains("Invalid schema line"));
    }

    @Test
    void testGetSchema_emptyFile() throws Exception {
        Files.writeString(Paths.get(TEST_SCHEMA_PATH), "");

        List<Triple> schema = Generator.getSchema(TEST_SCHEMA_PATH);

        assertTrue(schema.isEmpty());
    }

    @Test
    void testFixedLengthParserGenerator_containsExpectedSections() {
        String result = Generator.fixedLengthParserGenerator(TEST_SCHEMA_PATH);

        assertTrue(result.contains("class FixedLengthParser"));
        assertTrue(result.contains("parseFile"));
        assertTrue(result.contains("extractField"));
        assertTrue(result.contains("main"));
    }

    @Test
    void testFixedLengthParserGenerator_containsSchemaConstants() {
        String result = Generator.fixedLengthParserGenerator(TEST_SCHEMA_PATH);

        assertTrue(result.contains("NAME_START"));
        assertTrue(result.contains("AGE_START"));
        assertTrue(result.contains("CITY_START"));
    }

    @Test
    void testRecordGenerator_containsFields() {
        String result = Generator.recordGenerator();

        assertTrue(result.contains("private String name"));
        assertTrue(result.contains("private String age"));
        assertTrue(result.contains("private String city"));
    }

    @Test
    void testRecordGenerator_containsConstructor() {
        String result = Generator.recordGenerator();

        assertTrue(result.contains("public Record("));
        assertTrue(result.contains("this.name = name"));
    }

    @Test
    void testRecordGenerator_containsToString() {
        String result = Generator.recordGenerator();

        assertTrue(result.contains("toString"));
        assertTrue(result.contains("Record{"));
    }

    @Test
    void testFixedLengthParserGenerator_handlesSchemaException() throws IOException {
        Files.writeString(Paths.get(TEST_SCHEMA_PATH), "bad data");

        RuntimeException ex = assertThrows(RuntimeException.class,
                ()->Generator.fixedLengthParserGenerator(TEST_SCHEMA_PATH));

        assertTrue(ex.getMessage().contains("Error generating parser"));
    }

    @Test
    void testRecordGenerator_handlesSchemaException() throws IOException {
        Files.writeString(Paths.get(TEST_SCHEMA_PATH), "bad data");

        RuntimeException ex = assertThrows(RuntimeException.class,
                Generator::recordGenerator);
        System.out.println(ex.getMessage());

        assertTrue(ex.getMessage().contains("Error generating Record class"));
    }
}