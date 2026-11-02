package org.matsim.contrib.msp.capability;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilities;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilityExtractor;
import org.matsim.contrib.msp.capability.prepare.CapabilityPersonFilter;
import org.matsim.contrib.msp.capability.prepare.DefaultCapabilityPersonFilter;
import org.matsim.contrib.msp.capability.prepare.DiscretionaryActivityFacilityCsv;
import org.matsim.contrib.msp.capability.result.CapabilityWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.matsim.core.utils.geometry.transformations.TransformationFactory.ATLANTIS;

/**
 * Runs the {@link CapabilityAnalysis} on a finished MATSim run:
 * <pre>
 *   java -cp matsim-msp.jar org.matsim.contrib.msp.capability.RunCapabilityAnalysis \
 *       --input-directory output/ --places large-supermarkets.csv
 * </pre>
 * The input directory is anything that holds a config, a network and a plans file - the output directory
 * of a run, or a folder prepared by hand. The files are found by name, so the usual MATSim output naming
 * works without further arguments; {@code --config}, {@code --network} and {@code --plans} override the
 * guesses.
 * <p>
 * The places for the discretionary activities come from a csv of their own (see
 * {@link DiscretionaryActivityFacilityCsv}) and are kept in a <b>standalone facilities container</b>, never
 * merged into the scenario's own facilities. That keeps the two apart for good reasons: the scenario's
 * facilities carry MATSim activity types while these carry the capability vocabulary, ids cannot collide,
 * the run's own data is not modified, and - most importantly - the fallback in
 * {@link DiscretionaryActivityFacilityExtractor} that reads types from activity options would otherwise sweep
 * every facility of the scenario into the analysis.
 * <p>
 * Which persons are analysed is up to {@link DefaultCapabilityPersonFilter}: pass the study area as
 * {@code --shp} to keep its residents only, {@code --min-age} to set the youngest age kept, {@code --subpopulation} to say which subpopulations are
 * persons rather than freight.
 * <p>
 * The output is the three levels of {@link CapabilityWriter}, written into the input directory unless
 * {@code --output} says otherwise.
 */
@CommandLine.Command(
	name = "capability-analysis",
	description = "Measures the capability of each person by the discretionary activities that fit into their day.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public final class RunCapabilityAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunCapabilityAnalysis.class);

	/**
	 * The sub directory of the input directory the levels are written to, unless {@code --output} says
	 * otherwise. A directory of its own so that a second run of the analysis over the same output
	 * directory does not scatter its tables among the files of the run.
	 */
	public static final String OUTPUT_DIRECTORY = "capability-analysis";

	@CommandLine.Option(names = {"--input-directory", "--directory"}, description = "Directory holding the config, network and plans of "
		+ "a run, e.g. its output directory.", required = true)
	private String mainDirectory;

	@CommandLine.Option(names = "--places", description = "Csv of the places where the discretionary activities can be "
		+ "performed. See DiscretionaryActivityFacilityCsv for the columns.", required = true)
	private String placesFile;

	@CommandLine.Option(names = "--config", description = "Config file, if it should not be looked up in the main directory.")
	private String configFile;

	@CommandLine.Option(names = "--network", description = "Network file, if it should not be looked up in the main directory.")
	private String networkFile;

	@CommandLine.Option(names = "--plans", description = "Plans file, if it should not be looked up in the main directory.")
	private String plansFile;

	@CommandLine.Option(names = "--vehicles", description = "Vehicles file, if it should not be looked up in the main "
		+ "directory. It carries the speed of the network modes: the maximumVelocity of the vehicle type named after "
		+ "a mode is how fast that mode goes, so without it bike travels at the free speed of the links.")
	private String vehiclesFile;

	@CommandLine.Option(names = "--output", description = "Directory the three output levels are written to. "
		+ "Default is the main directory.")
	private String output;

	// not a negatable --insertions: picocli's negatable options invert the DEFAULT rather than the flag,
	// so with a default of true it is '--insertions' that would switch the level off. An explicit
	// --skip-insertions cannot be misread.
	@CommandLine.Option(names = "--skip-insertions", description = "Leave the insertion level out, i.e. the row per "
		+ "person, mode, type, facility and stretch of the day. It is by far the largest table - persons x modes x "
		+ "types x facilities x periods - so a run over a whole population that only needs the upper two levels is "
		+ "better off without it.")
	private boolean skipInsertions;

	@CommandLine.Option(names = "--value-of-time", description = "What one hour of travel is worth in utility, i.e. "
		+ "the VoT of c_insertion = VoT * t_extra. Overrides 'valueOfTimePerHour' of the capability config group.")
	private Double valueOfTimePerHour;

	@CommandLine.Option(names = "--write-facilities", description = "Also write the facilities built from the places csv, "
		+ "for checking what the analysis used.")
	private String writeFacilities;

	@CommandLine.Option(names = "--delimiter", description = "Column separator of the places csv.", defaultValue = ",")
	private char delimiter;

	@CommandLine.Option(names = "--project-crs", description = "Input coordinate system of the project", defaultValue = "EPSG:25832")
	private String inputCRS;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulations to analyse, so that freight and "
		+ "commercial agents stay out. Only applies to a population that carries subpopulations at all; pass an "
		+ "empty value to analyse every subpopulation.", defaultValue = DefaultCapabilityPersonFilter.DEFAULT_SUBPOPULATION,
		split = ",")
	private Set<String> subpopulations;

	@CommandLine.Option(names = "--min-age", description = "Youngest age to analyse, read from the 'age' person "
		+ "attribute, overriding the 'minAge' of the capability config. Persons without a readable age are "
		+ "dropped; pass 0 for a population that carries no ages.", defaultValue = "18")
	private Integer minAge;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	public static void main(String[] args) {
		new RunCapabilityAnalysis().execute(args);
	}

	@Override
	public Integer call() throws IOException {

		Path directory = Path.of(this.mainDirectory);
		if (!Files.isDirectory(directory)) {
			log.error("{} is not a directory.", directory);
			return 2;
		}

		// the config may or may not already carry the capability settings; either way we end up with a
		// config group holding the defaults for everything it does not specify
		String config = this.configFile != null ? this.configFile : find(directory, "config", ".xml");
		Config scenarioConfig = config != null
			? ConfigUtils.loadConfig(config, new CapabilityConfigGroup())
			: ConfigUtils.createConfig(new CapabilityConfigGroup());
		if (null == scenarioConfig.global().getCoordinateSystem() || ATLANTIS.equals(scenarioConfig.global().getCoordinateSystem())) {
			scenarioConfig.global().setCoordinateSystem(inputCRS);
		}
		CapabilityConfigGroup capabilityConfig = ConfigUtils.addOrGetModule(scenarioConfig, CapabilityConfigGroup.class);
		if (this.valueOfTimePerHour != null) {
			capabilityConfig.setValueOfTimePerHour(this.valueOfTimePerHour);
			log.info("Value of time set to {} utils per hour.", this.valueOfTimePerHour);
		}

		if (config == null) {
			log.warn("No config file found in {}, using defaults. Walk travel times need the routing config of the "
				+ "run, so pass --config if the teleported walk speed matters.", directory);
		}

		String network = this.networkFile != null ? this.networkFile : find(directory, "network", ".xml", ".xml.gz");
		String plans = this.plansFile != null ? this.plansFile : find(directory, "plans", ".xml", ".xml.gz");
		String vehicles = this.vehiclesFile != null ? this.vehiclesFile : find(directory, "vehicles", ".xml", ".xml.gz");

		if (network == null) {
			log.error("No network file found in {}. Pass --network.", directory);
			return 2;
		}
		if (plans == null) {
			log.error("No plans file found in {}. Pass --plans.", directory);
			return 2;
		}

		// point the config at the files we found, so that loading the scenario picks them up
		scenarioConfig.network().setInputFile(Path.of(network).toAbsolutePath().toString());
		scenarioConfig.plans().setInputFile(Path.of(plans).toAbsolutePath().toString());
		// the scenario's own facilities are not needed: the places come from the csv instead
		scenarioConfig.facilities().setInputFile(null);
		// the vehicle types carry the speed of the network modes, see CapabilityTravelTimes#maxSpeed
		if (vehicles != null) {
			scenarioConfig.vehicles().setVehiclesFile(Path.of(vehicles).toAbsolutePath().toString());
		}

		log.info("Config:   {}", config);
		log.info("Network:  {}", network);
		log.info("Plans:    {}", plans);
		log.info("Vehicles: {}", vehicles);
		log.info("Places:   {}", this.placesFile);

		Scenario scenario = ScenarioUtils.loadScenario(scenarioConfig);

		// the places get their own container. They are not snapped to any link here: every mode places
		// them in its own network, by coordinate, see CapabilityTravelTimes.FreeSpeedNetwork#link
		DiscretionaryActivityFacilityCsv csv = new DiscretionaryActivityFacilityCsv(capabilityConfig).setDelimiter(this.delimiter);
		ActivityFacilities facilities = csv.read(this.placesFile);
		log.info("Read {} places.", facilities.getFacilities().size());

		if (this.writeFacilities != null) {
			new FacilitiesWriter(facilities).write(this.writeFacilities);
			log.info("Facilities written to {}.", this.writeFacilities);
		}

		DiscretionaryActivityFacilities places = new DiscretionaryActivityFacilityExtractor(capabilityConfig).extract(facilities);

		new CapabilityAnalysis(capabilityConfig,
			personFilter(capabilityConfig, scenarioConfig.global().getCoordinateSystem()))
			.setWriteInsertions(!this.skipInsertions)
			.run(scenario, places, outputDirectory(directory));

		return 0;
	}

	/**
	 * The default filter. The study area is indexed in the coordinate system of the population, so that
	 * the home coordinates can be queried as they are.
	 */
	private CapabilityPersonFilter personFilter(CapabilityConfigGroup capabilityConfig, String populationCRS) {

		// an empty --subpopulation arrives as a blank entry rather than as an empty set
		Set<String> keep = this.subpopulations.stream()
			.map(String::trim)
			.filter(subpopulation -> !subpopulation.isEmpty())
			.collect(Collectors.toCollection(LinkedHashSet::new));

		int age = this.minAge != null ? this.minAge : capabilityConfig.getMinAge();

		if (!this.shp.isDefined()) {
			log.warn("No --shp given, so the analysis is not restricted to the residents of a study area.");
			return new DefaultCapabilityPersonFilter(capabilityConfig, null, keep, age);
		}

		return new DefaultCapabilityPersonFilter(capabilityConfig,
			this.shp.createIndex(populationCRS, "_"), keep, age);
	}

	private Path outputDirectory(Path directory) throws IOException {
		Path outputPath = this.output != null ? Path.of(this.output) : directory.resolve(OUTPUT_DIRECTORY);
		Files.createDirectories(outputPath);
		return outputPath;
	}

	/**
	 * Looks for the usual MATSim output naming, e.g. {@code output_network.xml.gz} for "network".
	 *
	 * @return the matching file, preferring the shortest name so that {@code output_plans.xml.gz} wins
	 * over {@code output_experienced_plans.xml.gz}; null if there is none
	 */
	private static String find(Path directory, String kind, String... suffixes) {
		try (Stream<Path> files = Files.list(directory)) {
			List<Path> candidates = files
				.filter(Files::isRegularFile)
				.filter(file -> {
					String name = file.getFileName().toString().toLowerCase();
					return name.contains(kind) && Stream.of(suffixes).anyMatch(name::endsWith);
				})
				.sorted((a, b) -> {
					int byLength = Integer.compare(a.getFileName().toString().length(),
						b.getFileName().toString().length());
					return byLength != 0 ? byLength : a.compareTo(b);
				})
				.toList();

			Optional<Path> found = candidates.stream().findFirst();
			if (candidates.size() > 1) {
				log.info("Several files could be the {}: {}. Using {}.", kind,
					candidates.stream().map(p -> p.getFileName().toString()).toList(),
					found.orElseThrow().getFileName());
			}
			return found.map(Path::toString).orElse(null);

		} catch (IOException e) {
			throw new UncheckedIOException("Could not list " + directory, e);
		}
	}
}
