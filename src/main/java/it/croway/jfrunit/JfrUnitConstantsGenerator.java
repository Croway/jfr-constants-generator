package it.croway.jfrunit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
	static ObjectMapper MAPPER = new ObjectMapper();
	static Logger LOGGER = LoggerFactory.getLogger(JfrUnitConstantsGenerator.class);
	static String PACKAGE = "org.moditect.jfrunit.events";

	public static void main(String[] args) throws IOException, TemplateException {
		JfrDoc jrfDoc = MAPPER.readValue(JfrUnitConstantsGenerator.class.getResourceAsStream("/jdk17-events.json"), JfrDoc.class);

		LOGGER.info("generating sources for version {} and distribution {}", jrfDoc.getVersion(), jrfDoc.getDistribution());

		Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
		cfg.setDirectoryForTemplateLoading(new File("src/main/resources/templates"));
		// Recommended settings for new projects:
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setFallbackOnNullLoopVariable(false);

		Template temp = cfg.getTemplate("jdk-event.ftlh");

		/* Merge data-model with template */
		File dir = new File("target/generated-sources/" + PACKAGE.replace(".", "/"));
		dir.mkdirs();
		for(Event event : jrfDoc.getEvents()) {
			File file = new File(dir.getPath() + "/" + event.getName() + ".java");
			file.createNewFile();
			try (FileOutputStream fos = new FileOutputStream(file);
				 	Writer out = new OutputStreamWriter(fos);) {
				Map root = new HashMap();
				root.put("package", PACKAGE);
				root.put("event", event);

				temp.process(root, out);
			}
		}

		temp = cfg.getTemplate("event-types.ftlh");

		File file = new File(dir.getPath() + "/" + "JfrEventTypes.java");
		file.createNewFile();
		try (FileOutputStream fos = new FileOutputStream(file);
			 Writer out = new OutputStreamWriter(fos);) {
			Map root = new HashMap();
			root.put("package", PACKAGE);
			root.put("events", jrfDoc.getEvents());

			temp.process(root, out);
		}

		temp = cfg.getTemplate("type.ftlh");
		dir = new File(dir.getPath() + "/" + "model");
		dir.mkdir();
		for(Type type : jrfDoc.getTypes()) {
			if(!baseTypes.contains(type.getName())) {
				file = new File(dir.getPath() + "/" + type.getName() + ".java");
				file.createNewFile();
				try (FileOutputStream fos = new FileOutputStream(file);
					 Writer out = new OutputStreamWriter(fos);) {
					Map root = new HashMap();
					root.put("package", PACKAGE);
					root.put("type", type);

					temp.process(root, out);
				}
			}
		}
	}

	static {
		MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
}