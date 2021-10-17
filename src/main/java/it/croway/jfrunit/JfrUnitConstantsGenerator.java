package it.croway.jfrunit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import it.croway.jfrunit.events.model.Event;
import it.croway.jfrunit.events.model.JfrDoc;
import it.croway.jfrunit.events.model.Type;

public class JfrUnitConstantsGenerator {

	static List<String> baseTypes = new ArrayList<>();
	static ObjectMapper MAPPER = new ObjectMapper();
	static Logger LOGGER = LoggerFactory.getLogger(JfrUnitConstantsGenerator.class);
	static String PACKAGE = "org.moditect.jfrunit.events";
	static String BASE_FOLDER_GEN = "src/main/java/";

	public static void generate(String jfrDocUrl, ProcessingEnvironment processingEnvironment) throws IOException, TemplateException {
		if (jfrDocUrl == null) {
			String error = "missing jfr-doc json argument (ex https://bestsolution-at.github.io/jfr-doc/openjdk-17.json)";
			LOGGER.error(error);
			throw new IllegalArgumentException(error);
		}

		JfrDoc jrfDoc = MAPPER.readValue(new BufferedInputStream(new URL(jfrDocUrl).openStream()), JfrDoc.class);

		LOGGER.info("generating sources for version {} and distribution {}", jrfDoc.getVersion(), jrfDoc.getDistribution());

		Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
		cfg.setClassForTemplateLoading(JfrUnitConstantsGenerator.class, "/templates");
		// Recommended settings for new projects:
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setFallbackOnNullLoopVariable(false);

		Template freemarkerTemplate = cfg.getTemplate("jdk-event.ftlh");
		for (Event event : jrfDoc.getEvents()) {
			JavaFileObject builderFile = processingEnvironment.getFiler().createSourceFile(PACKAGE + "." + event.getName());
			try (Writer out = builderFile.openWriter()) {
				Map root = new HashMap();
				root.put("package", PACKAGE);
				root.put("event", event);

				freemarkerTemplate.process(root, out);
			}
		}

		freemarkerTemplate = cfg.getTemplate("event-types.ftlh");
		JavaFileObject builderFile = processingEnvironment.getFiler().createSourceFile(PACKAGE + ".JfrEventTypes");
		try (Writer out = builderFile.openWriter()) {
			Map root = new HashMap();
			root.put("package", PACKAGE);
			root.put("events", jrfDoc.getEvents());

			freemarkerTemplate.process(root, out);
		}

		freemarkerTemplate = cfg.getTemplate("type.ftlh");
		for (Type type : jrfDoc.getTypes()) {
			if (!baseTypes.contains(type.getName())) {
				builderFile = processingEnvironment.getFiler().createSourceFile(PACKAGE + ".model." + type.getName());
				try (Writer out = builderFile.openWriter()) {
					Map root = new HashMap();
					root.put("package", PACKAGE);
					root.put("type", type);

					freemarkerTemplate.process(root, out);
				}
			}
		}

		LOGGER.info("sources generated in {} folder", BASE_FOLDER_GEN);
	}

	static {
		baseTypes.add("boolean");
		baseTypes.add("byte");
		baseTypes.add("char");
		baseTypes.add("double");
		baseTypes.add("float");
		baseTypes.add("int");
		baseTypes.add("long");
		baseTypes.add("short");
		baseTypes.add("String");
		baseTypes.add("char");
	}

	static {
		MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
}