
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

public class TemplateEngineFactory {

    public TemplateEngine create() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode("RAW");
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}

