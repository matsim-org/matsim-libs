package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.TripsAndLegsWriter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes the trips and legs CSV files of a finished run, using {@link TripsAndLegsWriter} with its default settings.
 * <p>
 * Only the output files of the run directory are needed, no config: the experienced plans, the network and, if present,
 * the facilities. The default config is used, so the files are written with the default delimiter.
 * <p>
 * The trips file is the same as the one written during the simulation. In the legs file, the columns {@code vehicle_id}
 * and {@code network_mode} stay empty, because the vehicle id of a leg is not stored in the experienced plans file.
 * <p>
 * If no output path is given, the files are written to the run directory and compressed like the experienced plans file
 * that was found, e.g. {@code <run directory>/trips.csv.zst} for {@code output_experienced_plans.xml.zst}. Output paths
 * that are given are used as they are.
 */
@CommandLine.Command(name = "write-trips-and-legs",
	description = "Writes the trips and legs CSV files of a finished run from its output experienced plans. " +
		"By default, the files are written to the run directory and compressed like the experienced plans file.")
@CommandSpec(requireRunDirectory = true, produces = {"trips.csv", "legs.csv"})
public class TripsAndLegsCsvWriter implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(TripsAndLegsCsvWriter.class);

	/**
	 * File extensions of the compressions that {@link org.matsim.core.utils.io.IOUtils} can read and write.
	 */
	private static final Pattern COMPRESSION_EXTENSION = Pattern.compile("\\.(gz|lz4|bz2|zst|zip)$");

	@CommandLine.Spec
	private CommandLine.Model.CommandSpec spec;

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(TripsAndLegsCsvWriter.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(TripsAndLegsCsvWriter.class);

	public static void main(String[] args) {
		new TripsAndLegsCsvWriter().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Path runDirectory = input.getRunDirectory();

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Path networkPath = ApplicationUtils.matchInput("network.xml", runDirectory);
		log.info("Reading network from {}", networkPath);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath.toString());

		Optional<Path> facilitiesPath = matchOptionalInput("facilities.xml", runDirectory);
		if (facilitiesPath.isPresent()) {
			log.info("Reading facilities from {}", facilitiesPath.get());
			new MatsimFacilitiesReader(scenario).readFile(facilitiesPath.get().toString());
		} else {
			log.info("No facilities file found in {}, coordinates are taken from the activities and the network only.", runDirectory);
		}

		Path experiencedPlansPath = ApplicationUtils.matchInput("experienced_plans.xml", runDirectory);
		log.info("Reading experienced plans from {}", experiencedPlansPath);
		new PopulationReader(scenario).readFile(experiencedPlansPath.toString());

		Map<Id<Person>, Plan> experiencedPlans = new LinkedHashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (person.getSelectedPlan() != null) {
				experiencedPlans.put(person.getId(), person.getSelectedPlan());
			}
		}

		String compression = compressionExtension(experiencedPlansPath);
		Path tripsPath = outputPath("output_trips.csv", "--output-trips", compression);
		Path legsPath = outputPath("output_legs.csv", "--output-legs", compression);
		log.info("Writing trips to {} and legs to {}", tripsPath, legsPath);
		new TripsAndLegsWriter(scenario).write(experiencedPlans, tripsPath.toString(), legsPath.toString());

		return 0;
	}

	/**
	 * Returns the output path for {@code name}. If the path was not given on the command line, the file is put into the run
	 * directory and the compression extension is appended.
	 */
	Path outputPath(String name, String option, String compression) {
		boolean isGiven = !spec.findOption(option).originalStringValues().isEmpty();
		return isGiven ? output.getPath(name) : input.getRunDirectory().resolve(name + compression);
	}

	/**
	 * Returns the compression extension of a file including the leading dot, e.g. {@code ".zst"}, or an empty string if the
	 * file is not compressed.
	 */
	static String compressionExtension(Path file) {
		Matcher matcher = COMPRESSION_EXTENSION.matcher(file.getFileName().toString());
		return matcher.find() ? matcher.group() : "";
	}

	private static Optional<Path> matchOptionalInput(String name, Path runDirectory) {
		try {
			return Optional.of(ApplicationUtils.matchInput(name, runDirectory));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
