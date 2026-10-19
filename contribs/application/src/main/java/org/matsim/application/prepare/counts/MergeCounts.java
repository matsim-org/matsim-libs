package org.matsim.application.prepare.counts;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimAppCommand;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Measurable;
import org.matsim.counts.MeasurementLocation;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges several MATSim counts files into one, e.g. the counts of the different Berlin data sources, which each cover
 * their own part of the network and are created by their own command.
 * <p>
 * Merging happens per measurement location and, within a location, per measurable, i.e. per combination of type
 * ("volumes", "velocities", ...) and network mode. Two files contributing different modes for the same link therefore
 * end up as one location holding both, which is what the counts format expects.
 */
@CommandLine.Command(name = "merge-counts", description = "Merges multiple counts files into a single one")
public class MergeCounts implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(MergeCounts.class);

	@CommandLine.Parameters(arity = "2..*", paramLabel = "COUNTS", description = "Counts files to merge, given in " +
		"decreasing priority: with --on-conflict=KEEP_FIRST the file named first wins a contested measurable.")
	private List<Path> input;

	@CommandLine.Option(names = "--output", description = "Path of the merged counts file", required = true)
	private Path output;

	@CommandLine.Option(names = "--on-conflict", defaultValue = "FAIL", description = "What to do if two files hold " +
		"the same measurable, i.e. the same type and mode, for the same link. Candidates: ${COMPLETION-CANDIDATES}. " +
		"Default: ${DEFAULT-VALUE}")
	private ConflictStrategy onConflict;

	@CommandLine.Option(names = "--name", description = "Name of the merged counts, taken from the first input if unset")
	private String name;

	@CommandLine.Option(names = "--description", description = "Description of the merged counts, taken from the first input if unset")
	private String description;

	@CommandLine.Option(names = "--source", description = "Source of the merged counts, taken from the first input if unset")
	private String source;

	@CommandLine.Option(names = "--year", description = "Year of the merged counts, taken from the first input if unset")
	private Integer year;

	public static void main(String[] args) {
		new MergeCounts().execute(args);
	}

	/**
	 * Copies a location that the merged counts do not hold yet, including its metadata and all of its measurables.
	 */
	private static void copyLocation(Counts<Link> target, MeasurementLocation<Link> from) {

		MeasurementLocation<Link> to = target.createAndAddMeasureLocation(from.getRefId(), from.getStationName());

		to.setId(from.getId());
		to.setDescription(from.getDescription());
		to.setCoordinates(from.getCoordinates());
		from.getAttributes().getAsMap().forEach(to.getAttributes()::putAttribute);

		for (MeasurementLocation.TypeAndMode typeAndMode : from) {
			copyMeasurable(to, from.getMeasurableForMode(typeAndMode.type(), typeAndMode.mode()));
		}
	}

	/**
	 * Copies one measurable with all of its values. The interval is carried over, so that a file of quarter hourly
	 * values does not silently become an hourly one.
	 */
	private static void copyMeasurable(MeasurementLocation<Link> to, Measurable from) {

		Measurable created = to.createMeasurable(from.getMeasurableType(), from.getMode(), from.getInterval());

		for (Int2DoubleMap.Entry value : from) {
			created.setAtSecond(value.getIntKey(), value.getDoubleValue());
		}
	}

	@Override
	public Integer call() throws Exception {

		//Reading everything before writing keeps a conflict from leaving a half written output file behind
		Map<Path, Counts<Link>> inputs = new LinkedHashMap<>();
		for (Path path : input) {

			if (!Files.exists(path))
				throw new IllegalArgumentException("Counts file " + path + " does not exist.");

			if (inputs.containsKey(path))
				throw new IllegalArgumentException("Counts file " + path + " is given more than once.");

			Counts<Link> counts = new Counts<>();
			new MatsimCountsReader(counts).readFile(path.toString());

			log.info("Read {} locations from {}", counts.getMeasureLocations().size(), path);
			inputs.put(path, counts);
		}

		Counts<Link> merged = new Counts<>();
		mergeMetadata(merged, inputs);

		for (Map.Entry<Path, Counts<Link>> entry : inputs.entrySet()) {
			merge(merged, entry.getValue(), entry.getKey());
		}

		if (merged.getMeasureLocations().isEmpty())
			throw new IllegalArgumentException("The merged counts are empty, none of the input files holds a location.");

		if (output.getParent() != null)
			Files.createDirectories(output.getParent());

		new CountsWriter(merged).write(output.toString());

		log.info("Wrote {} locations to {}", merged.getMeasureLocations().size(), output);

		return 0;
	}

	/**
	 * Takes the counts wide metadata from the first input, unless it is given on the command line. A disagreement
	 * between the inputs is only logged: counts of different years are a questionable but deliberate thing to merge,
	 * and the command is not in a position to decide which year the result should carry.
	 */
	private void mergeMetadata(Counts<Link> merged, Map<Path, Counts<Link>> inputs) {

		Counts<Link> first = inputs.values().iterator().next();

		merged.setName(name != null ? name : first.getName());
		merged.setDescription(description != null ? description : first.getDescription());
		merged.setSource(source != null ? source : first.getSource());
		merged.setYear(year != null ? year : first.getYear());

		if (year == null) {
			List<Integer> years = inputs.values().stream().map(Counts::getYear).distinct().toList();
			if (years.size() > 1)
				log.warn("The inputs carry different years {}, the merged counts are written with {}. Use --year to set it.",
					years, merged.getYear());
		}
	}

	/**
	 * Adds one input to the merged counts and reports what it contributed.
	 */
	private void merge(Counts<Link> merged, Counts<Link> counts, Path path) {

		int added = 0;
		int extended = 0;
		int measurables = 0;
		List<String> conflicts = new ArrayList<>();

		for (MeasurementLocation<Link> location : counts.getMeasureLocations().values()) {

			MeasurementLocation<Link> existing = merged.getMeasureLocation(location.getRefId());

			if (existing == null) {
				copyLocation(merged, location);
				added++;
				continue;
			}

			//The same link measured by two sources. Their station names usually differ, because every source names its
			//stations itself, so the name of the location that got there first is kept
			boolean contributed = false;
			for (MeasurementLocation.TypeAndMode typeAndMode : location) {

				Measurable measurable = location.getMeasurableForMode(typeAndMode.type(), typeAndMode.mode());

				if (existing.hasMeasurableForMode(typeAndMode.type(), typeAndMode.mode())) {

					conflicts.add("%s (%s, %s)".formatted(location.getRefId(), typeAndMode.type(), typeAndMode.mode()));

					if (onConflict == ConflictStrategy.KEEP_FIRST)
						continue;

					if (onConflict == ConflictStrategy.KEEP_LAST) {
						//createMeasurable returns the existing instance, so the old values have to go first, otherwise
						//the two sets of values would be mixed hour by hour
						existing.deleteMeasurable(typeAndMode.type(), typeAndMode.mode());
					}
				}

				copyMeasurable(existing, measurable);
				measurables++;
				contributed = true;
			}

			if (contributed)
				extended++;
		}

		if (!conflicts.isEmpty()) {

			String message = "%d measurable(s) of %s are already present from an earlier file, e.g. %s."
				.formatted(conflicts.size(), path, conflicts.subList(0, Math.min(5, conflicts.size())));

			if (onConflict == ConflictStrategy.FAIL)
				throw new IllegalArgumentException(message + " Use --on-conflict to choose which one to keep.");

			log.warn("{} Keeping the {} one.", message, onConflict == ConflictStrategy.KEEP_FIRST ? "first" : "last");
		}

		log.info("Merged {}: {} new locations, {} measurable(s) added to {} existing locations",
			path, added, measurables, extended);
	}

	/**
	 * What to do if two inputs hold the same measurable for the same link.
	 */
	public enum ConflictStrategy {
		/**
		 * Refuse to merge. Counts drive the calibration, so silently dropping one of two disagreeing measurements is
		 * not a good default.
		 */
		FAIL,
		/**
		 * Keep the value of the file named first, i.e. treat the input order as a priority order.
		 */
		KEEP_FIRST,
		/**
		 * Keep the value of the file named last, i.e. let a later file override an earlier one.
		 */
		KEEP_LAST
	}
}
