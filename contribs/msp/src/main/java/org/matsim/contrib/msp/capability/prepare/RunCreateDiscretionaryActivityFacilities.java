package org.matsim.contrib.msp.capability.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilities;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilityExtractor;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;
import picocli.CommandLine;

/**
 * Turns a csv of places into a MATSim facilities file the capability analysis can work with:
 * <pre>
 *   java -cp matsim-msp.jar org.matsim.contrib.msp.capability.RunCreateDiscretionaryActivityFacilities \
 *       --input places.csv --output discretionary_activity_facilities.xml.gz --crs EPSG:25833
 * </pre>
 * The csv is described by {@link DiscretionaryActivityFacilityCsv}. The output is an ordinary MATSim
 * facilities file — annotated so that {@link DiscretionaryActivityFacilityExtractor} finds the types,
 * sub-types and opening hours again, but otherwise usable by the rest of MATSim.
 * <p>
 * The csv is the single entry point on purpose. A source of places that is not a hand maintained list —
 * an OpenStreetMap extract, as the accessibility contrib reads one — should write that csv and then go
 * through this command, rather than build facilities of its own. That keeps the mapping from a source
 * to the capability vocabulary separate from, and inspectable before, the facility generation.
 *
 * @see DiscretionaryActivityFacilityCsv for the columns
 */
@CommandLine.Command(
	name = "create-discretionary-activity-facilities",
	description = "Creates a MATSim facilities file of the places at which discretionary activities can be performed.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public final class RunCreateDiscretionaryActivityFacilities implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunCreateDiscretionaryActivityFacilities.class);

	@CommandLine.Option(names = "--input", description = "Path to the csv of places to read.", required = true)
	private String input;

	@CommandLine.Option(names = "--output", description = "Path of the facilities file to write.", required = true)
	private String output;

	@CommandLine.Option(names = "--delimiter", description = "Column separator of the csv.", defaultValue = ",")
	private char delimiter;

	@CommandLine.Option(names = "--crs", description = "Coordinate reference system of the x/y columns, e.g. EPSG:25833. "
		+ "Written into the facilities file so that MATSim knows what the coordinates mean.")
	private String crs;

	@CommandLine.Option(names = "--config", description = "Path to a config file the capability parameters, in particular "
		+ "the names of the facility attributes to write, are read from.")
	private String configFile;

	@CommandLine.Option(names = "--summary", description = "Path of a csv to write the places back to, for checking what "
		+ "was read. Not written by default.")
	private String summary;

	public static void main(String[] args) {
		new RunCreateDiscretionaryActivityFacilities().execute(args);
	}

	@Override
	public Integer call() {

		CapabilityConfigGroup config = ConfigUtils.addOrGetModule(
			this.configFile != null
				? ConfigUtils.loadConfig(this.configFile, new CapabilityConfigGroup())
				: ConfigUtils.createConfig(new CapabilityConfigGroup()),
			CapabilityConfigGroup.class);

		DiscretionaryActivityFacilityCsv csv = new DiscretionaryActivityFacilityCsv(config).setDelimiter(this.delimiter);

		ActivityFacilities facilities = csv.read(this.input);

		if (this.crs != null) {
			ProjectionUtils.putCRS(facilities, this.crs);
		} else {
			log.warn("No --crs given, so the facilities file does not say what its coordinates mean.");
		}

		// read the facilities back the way the analysis will, so what is logged is what it will see
		DiscretionaryActivityFacilities places = new DiscretionaryActivityFacilityExtractor(config).extract(facilities);
		logSummary(places);

		new FacilitiesWriter(facilities).write(this.output);
		log.info("Facilities written to {}.", this.output);

		if (this.summary != null) {
			csv.write(places, this.summary);
		}

		return 0;
	}

	private static void logSummary(DiscretionaryActivityFacilities places) {
		log.info("{} places of {} types:", places.size(), places.getTypes().size());
		for (String type : places.getTypes()) {
			log.info("  {}: {} places, {} sub-types {}", type, places.getByType(type).size(),
				places.getSubTypes(type).size(), places.getSubTypes(type));
		}
	}
}
