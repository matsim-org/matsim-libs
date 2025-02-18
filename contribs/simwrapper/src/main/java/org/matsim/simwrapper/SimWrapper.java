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
import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.CommandRunner;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.simwrapper.viz.Viz;
import tech.tablesaw.plotly.components.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Class to define and generate SimWrapper dashboards.
 */
@SuppressWarnings("unused")
public final class SimWrapper {

	private static final Logger log = LogManager.getLogger(SimWrapper.class);

	private final Data data;

	private final Config config = new Config();

	private final org.matsim.core.config.Config matsimConfig;
	private final SimWrapperConfigGroup configGroup;

	private final List<Dashboard> dashboards = new ArrayList<>();

	/**
	 * Use {@link #create(org.matsim.core.config.Config)}.
	 */
	private SimWrapper(org.matsim.core.config.Config config) {
		this.matsimConfig = config;
		this.configGroup = ConfigUtils.addOrGetModule(matsimConfig, SimWrapperConfigGroup.class);
		this.data = new Data(config.getContext(), configGroup);
	}

	/**
	 * Create a new {@link SimWrapper} instance with default config.
	 */
	public static SimWrapper create() {
		return new SimWrapper(ConfigUtils.createConfig());
	}

	/**
	 * * Create a new {@link SimWrapper} instance with given config.
	 */
	public static SimWrapper create(org.matsim.core.config.Config config) {
		return new SimWrapper(config);
	}

	/**
	 * Utility method to define a binding for a dashboard. These will be picked up if the MATSim integration is used.
	 */
	public static LinkedBindingBuilder<Dashboard> addDashboardBinding(Binder binder) {
		return Multibinder.newSetBinder(binder, Dashboard.class).addBinding();
	}

	/**
	 * Return the {@link Data} instance for managing.
	 */
	public Data getData() {
		return data;
	}

	/**
	 * Get the internal simwrapper config, which will be exported as yaml file.
	 */
	public Config getConfig() {
		return config;
	}

	/**
	 * Return associated config group.
	 */
	public SimWrapperConfigGroup getConfigGroup() {
		return configGroup;
	}

	/**
	 * Adds a dashboard definition to SimWrapper.
	 * This only stores the specification, the actual code is executed during {@link #generate(Path)}.
	 */
	public SimWrapper addDashboard(Dashboard d) {
		dashboards.add(d);
		return this;
	}

	/**
	 * Check if dashboard of same type is present already.
	 */
	boolean hasDashboard(Class<? extends Dashboard> d, String context) {
		return dashboards.stream().anyMatch(o -> d.isAssignableFrom(o.getClass()) && Objects.equals(o.context(), context));
	}

	/**
	 * Generate the dashboards specification and writes .yaml files to {@code dir}.
	 */
	public void generate(Path dir) throws IOException {
		generate(dir, false);
	}

	/**
	 * Generate the dashboards specification and writes .yaml files to {@code dir}.
	 * @param dir target directory
	 * @param append if true, existing dashboards will not be overwritten
	 */
	public void generate(Path dir, boolean append) throws IOException {

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

		dashboards.sort(Comparator.comparingDouble(Dashboard::priority).reversed());

		int i = 1;
		for (Dashboard d : dashboards) {

			YAML yaml = new YAML();
			Layout layout = new Layout(d.context());

			d.configure(yaml.header, layout);
			yaml.layout = layout.create(data);
			yaml.subtabs = layout.getTabs();

			Path out = dir.resolve("dashboard-" + i + ".yaml");

			while (append && Files.exists(out)) {
				out = dir.resolve("dashboard-" + ++i + ".yaml");
			}

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
		run(dir, null);
	}
	/**
	 * Run the pipeline, and pass a different config file. This functionality is only available via {@link SimWrapperRunner}.
	 */
	void run(Path dir, String configPath) {

		for (Map.Entry<Path, URL> e : data.getResources().entrySet()) {
			try {
				Files.createDirectories(e.getKey().getParent());
				try (InputStream is = e.getValue().openStream()) {
					Files.copy(is, e.getKey());
				}

			} catch (IOException ex) {
				log.error("Could not copy resources", ex);
			}
		}

		for (CommandRunner runner : data.getRunners().values()) {

			SimWrapperConfigGroup.ContextParams ctx = configGroup.get(runner.getName());

			runner.setSampleSize(configGroup.sampleSize);

			if (configPath != null)
				runner.setConfigPath(configPath);

			if (ctx.shp != null) {

				try {
					URI path = ConfigGroup.getInputFileURL(matsimConfig.getContext(), ctx.shp).toURI();

					if (path.getScheme().equals("file"))
						runner.setShp(new File(path).getAbsoluteFile().toString());
					else
						runner.setShp(path.toString());

				} catch (URISyntaxException e) {
					log.warn("Could not set shp file", e);
				}
			}

			// Insert the globally defined arguments
			for (Map.Entry<Class<? extends MATSimAppCommand>, String[]> e : data.getGlobalArgs().entrySet()) {
				runner.insertArgs(e.getKey(), e.getValue());
			}

			runner.run(dir);
		}
	}

	/**
	 * This class stores the data as required in the yaml files.
	 */
	private static final class YAML {

		private final Header header = new Header();
		private Collection<Layout.Tab> subtabs;
		private Map<String, List<Viz>> layout;

	}

	/**
	 * Class representing the simwrapper config.
	 */
	public static final class Config {

		public boolean hideLeftBar = false;
		public boolean hideBreadcrumbs = false;
		public boolean hideFiles = false;
		public boolean fullWidth = true;

		public String header;
		public String footer_en;
		public String footer_de;
		public String css;
		public String theme;

	}
}
