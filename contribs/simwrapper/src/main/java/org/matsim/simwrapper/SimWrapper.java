package org.matsim.simwrapper;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimAppCommand;
import org.matsim.simwrapper.viz.Viz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class to define and generate SimWrapper dashboards.
 */
public final class SimWrapper {

	private static final Logger log = LogManager.getLogger(SimWrapper.class);

	public final Data data = new Data();
	private final List<Dashboard> dashboards = new ArrayList<>();

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

	/**
	 * Adds an dashboard definition to SimWrapper.
	 * This only stores the specification, the actual code is executed during {@link #generate(Path)}.
	 */
	public SimWrapper addDashboard(Dashboard d) {
		dashboards.add(d);
		return this;
	}

	/**
	 * Add dashboard at specific index.
	 *
	 * @see #addDashboard(Dashboard)
	 */
	public SimWrapper addDashboard(int index, Dashboard d) {
		dashboards.add(index, d);
		return this;
	}

	/**
	 * Generate the dashboards specification and writes .yaml files to {@code dir}.
	 */
	public void generate(Path dir) throws IOException {

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER).enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		ObjectWriter writer = mapper.writerFor(YAML.class);

		Path target = dir.resolve(".simwrapper");
		Files.createDirectories(target);

		data.setPath(dir);

		// TODO: copy config, css, and other auxiliary files
		// from resources

		int i = 0;
		for (Dashboard d : dashboards) {

			YAML yaml = new YAML();
			Layout layout = new Layout();

			d.configure(yaml.header, layout);
			yaml.layout = layout.create(data);

			Path out = target.resolve("dashboard-" + i + ".yaml");
			writer.writeValue(out.toFile(), yaml);

			i++;
		}
	}

	/**
	 * Run data pipeline to create the necessary data for the dashboards.
	 */
	public void run() {
		for (MATSimAppCommand command : data.getCommands()) {
			try {
				command.call();
			} catch (Exception e) {
				log.error("Could not execute command {}", command, e);
			}
		}
	}

	/**
	 * This class stores the data as required in the yaml files.
	 */
	private static final class YAML {

		private final Header header = new Header();
		private Map<String, List<Viz>> layout;

	}

}
