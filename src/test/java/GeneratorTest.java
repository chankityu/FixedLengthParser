
import org.junit.jupiter.api.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {

    private static final String TEST_SCHEMA_PATH = "Interview_FixedLengthParser/schema.txt";

    private Generator generator;

    @BeforeEach
    void setup() throws IOException {
        Files.createDirectories(Paths.get("Interview_FixedLengthParser"));

        String schema = """
                name 1 10
                age 11 13
                city 14 25
                """;

        Files.writeString(Paths.get(TEST_SCHEMA_PATH), schema);

        // Setup template engine
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode("TEXT");
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);

        generator = new Generator(engine);
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(TEST_SCHEMA_PATH));
    }

    @Test
    void testGetSchema_validSchema() throws Exception {
        List<Triple> schema = Generator.getSchema(TEST_SCHEMA_PATH);

        assertEquals(3, schema.size());

        assertEquals("name", schema.get(0).getName());
        assertEquals(1, schema.get(0).getStart());
        assertEquals(10, schema.get(0).getEnd());
    }

    @Test
    void testGetSchema_invalidSchemaLine() throws IOException {
        Files.writeString(Paths.get(TEST_SCHEMA_PATH), "invalid line");

        Exception ex = assertThrows(
                IllegalArgumentException.class,
                () -> Generator.getSchema(TEST_SCHEMA_PATH)
        );

        assertTrue(ex.getMessage().contains("Invalid schema line"));
    }

    @Test
    void testGetSchema_emptyFile() throws Exception {
        Files.writeString(Paths.get(TEST_SCHEMA_PATH), "");

        List<Triple> schema = Generator.getSchema(TEST_SCHEMA_PATH);

        assertTrue(schema.isEmpty());
    }

    @Test
    void testGenerateParser_containsExpectedSections() throws Exception {
        List<Triple> schema = Generator.getSchema(TEST_SCHEMA_PATH);

        String result = generator.generateParser(schema);

        assertTrue(result.contains("class FixedLengthParser"));
        assertTrue(result.contains("parseFile"));
        assertTrue(result.contains("extractField"));
    }

    @Test
    void testGenerateParser_containsSchemaConstants() throws Exception {
        List<Triple> schema = Generator.getSchema(TEST_SCHEMA_PATH);

        String result = generator.generateParser(schema);

        assertTrue(result.contains("NAME_START"));
        assertTrue(result.contains("AGE_START"));
        assertTrue(result.contains("CITY_START"));
    }

    @Test
    void testGenerateRecord_containsFields() throws Exception {
        List<Triple> schema = Generator.getSchema(TEST_SCHEMA_PATH);

        String result = generator.generateRecord(schema);
        System.out.println(result);

        assertTrue(result.contains("private String name"));
        assertTrue(result.contains("private String age"));
        assertTrue(result.contains("private String city"));
    }

    @Test
    void testGeneratedRecordCodeShouldCompile() throws Exception {
        List<Triple> schema = Generator.getSchema(TEST_SCHEMA_PATH);

        String recordCode = generator.generateRecord(schema);
        System.out.println(recordCode);
        // Create temp directory
        File tempDir = Files.createTempDirectory("gen-test").toFile();

        //File parserFile = new File(tempDir, "FixedLengthParser.java");
        File recordFile = new File(tempDir, "Record.java");

        //writeToFile(parserFile, parserCode);
        writeToFile(recordFile, recordCode);

        //System.out.println(parserCode);

        // Compile
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Compiler not available. Are you using JDK?");

        int result = compiler.run(
                null,
                null,
                null,
                //parserFile.getPath(),
                recordFile.getPath()
        );

        // Assert
        assertEquals(0, result, "Generated code failed to compile");
    }

    @Test
    void testGeneratedParserCodeShouldCompile() throws Exception {
        List<Triple> schema = Generator.getSchema(TEST_SCHEMA_PATH);

        String parserCode = generator.generateParser(schema);
        System.out.println(parserCode);
        // Create temp directory
        File tempDir = Files.createTempDirectory("gen-test").toFile();

        File parserFile = new File(tempDir, "FixedLengthParser.java");

        //writeToFile(parserFile, parserCode);
        writeToFile(parserFile, parserCode);

        //System.out.println(parserCode);

        // Compile
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Compiler not available. Are you using JDK?");

        int result = compiler.run(
                null,
                null,
                null,
                parserFile.getPath()
        );

        // Assert
        assertEquals(0, result, "Generated code failed to compile");
    }

    private void writeToFile(File file, String content) throws Exception {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }


    @Test
    void testGenerateRecord_containsConstructor() throws Exception {
        List<Triple> schema = Generator.getSchema(TEST_SCHEMA_PATH);

        String result = generator.generateRecord(schema);

        assertTrue(result.contains("public Record("));
        assertTrue(result.contains("this.name = name"));
    }

    @Test
    void testGenerateRecord_containsToString() throws Exception {
        List<Triple> schema = Generator.getSchema(TEST_SCHEMA_PATH);

        String result = generator.generateRecord(schema);

        assertTrue(result.contains("toString"));
        assertTrue(result.contains("Record{"));
    }

    @Test
    void testGenerateParser_handlesSchemaException() throws IOException {
        Files.writeString(Paths.get(TEST_SCHEMA_PATH), "bad data");

        Exception ex = assertThrows(
                IllegalArgumentException.class,
                () -> Generator.getSchema(TEST_SCHEMA_PATH)
        );

        assertTrue(ex.getMessage().contains("Invalid schema line"));
    }
}

