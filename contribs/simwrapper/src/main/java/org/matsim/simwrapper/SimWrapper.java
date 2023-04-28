package org.matsim.simwrapper;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.CommandRunner;
import org.matsim.simwrapper.viz.Viz;
import tech.tablesaw.plotly.components.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class to define and generate SimWrapper dashboards.
 */
@SuppressWarnings("unused")
public final class SimWrapper {

	private static final Logger log = LogManager.getLogger(SimWrapper.class);

	private final Data data = new Data();

	private final Config config = new Config();

	private final List<Dashboard> dashboards = new ArrayList<>();
	private final List<String> context = new ArrayList<>();

	/**
	 * Use {@link #create()}.
	 */
	private SimWrapper() {
	}

	// TODO: simwrapper folder name

	public static SimWrapper create() {
		return new SimWrapper();
	}

	/**
	 * Return the {@link Data} instance for managing
	 */
	public Data getData() {
		return data;
	}

	// TODO: docs
	public Config getConfig() {
		return config;
	}

	/**
	 * Adds a dashboard definition to SimWrapper.
	 * This only stores the specification, the actual code is executed during {@link #generate(Path)}.
	 */
	public SimWrapper addDashboard(Dashboard d) {
		return addDashboard(d, "");
	}

	/**
	 * Adds a dashboard definition to SimWrapper.
	 * This only stores the specification, the actual code is executed during {@link #generate(Path)}.
	 *
	 * @param context context name, which allows to add multiple dashboards of the same kind.
	 */
	public SimWrapper addDashboard(Dashboard d, String context) {
		dashboards.add(d);
		this.context.add(context);
		return this;
	}

	/**
	 * Generate the dashboards specification and writes .yaml files to {@code dir}.
	 */
	public void generate(Path dir) throws IOException {

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
				.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
				.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		mapper.registerModule(new JavaTimeModule());
		mapper.addMixIn(Component.class, ComponentMixin.class);

		ObjectWriter writer = mapper.writerFor(YAML.class);

		Files.createDirectories(dir);

		data.setPath(dir.resolve("analysis"));

		// Initialize default context and path
		data.setCurrentContext("");

		// TODO: copy config, css, and other auxiliary files
		// from resources

		int i = 0;
		for (Dashboard d : dashboards) {

			YAML yaml = new YAML();
			Layout layout = new Layout(context.get(i));

			d.configure(yaml.header, layout);
			yaml.layout = layout.create(data);

			Path out = dir.resolve("dashboard-" + i + ".yaml");
			writer.writeValue(out.toFile(), yaml);

			i++;
		}

		ObjectWriter configWriter = mapper.writerFor(Config.class);

		Path out = dir.resolve("simwrapper-config.yaml");
		configWriter.writeValue(out.toFile(), config);

		// TODO: think about json schema for the datatypes
	}

	/**
	 * Run data pipeline to create the necessary data for the dashboards.
	 */
	public void run(Path dir) {
		for (CommandRunner runner : data.getRunners().values()) {
			runner.run(dir);
		}
	}

	/**
	 * This class stores the data as required in the yaml files.
	 */
	private static final class YAML {

		private final Header header = new Header();
		private Map<String, List<Viz>> layout;

	}

	/**
	 * Class representing the simwrapper config.
	 */
	public static final class Config {

		public boolean hideLeftBar = false;
		public boolean fullWidth = true;

	}
}
