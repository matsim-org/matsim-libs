// file: org/matsim/counts/MergeCounts.java
package org.matsim.counts;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;
import java.util.OptionalDouble;


public class CountsUtils {

	private static final Logger log = LogManager.getLogger(CountsUtils.class);

	/**
	 * Merges counts from the addition Counts object into the base Counts object.
	 *
	 * <p>For each measurement location in the addition counts file:
	 * <ul>
	 *   <li>If the location key does not exist in the base counts, the location is added.
	 *   <li>If the location key exists, the duplicate locations are merged according to the duplicate handling mode.
	 * </ul>
	 *
	 * @param base     The base Counts object that will hold the merged data.
	 * @param addition The Counts object providing additional data.
	 * @param mode     The duplicate handling mode: SKIP, OVERRIDE, or SUM.
	 */
	public static void mergeCounts(Counts<Link> base, Counts<Link> addition, DuplicateHandling mode) {
		// Log keys from the base counts file for reference.
		for (Map.Entry<Id<Link>, MeasurementLocation<Link>> entry : base.getMeasureLocations().entrySet()) {
			Id<Link> key = entry.getKey();
			log.info("Base location with key: " + key);
		}

		// Log keys from the addition counts file for reference.
		for (Map.Entry<Id<Link>, MeasurementLocation<Link>> entry : addition.getMeasureLocations().entrySet()) {
			Id<Link> key = entry.getKey();
			log.info("Addition location with key: " + key);
		}

		// Merge each location from the addition counts into the base counts.
		for (Map.Entry<Id<Link>, MeasurementLocation<Link>> entry : addition.getMeasureLocations().entrySet()) {
			Id<Link> key = entry.getKey();
			MeasurementLocation<Link> locToAdd = entry.getValue();

			log.info("Merging location with key: " + key);

			if (!base.getMeasureLocations().containsKey(key)) {
				// New location: add it directly.
				base.getMeasureLocations().put(key, locToAdd);
				log.info("Added new location with key: " + key);
			} else {
				log.info("Location with key " + key + " already exists in base counts file.");
				// Duplicate location found: merge based on the duplicate handling mode.
				MeasurementLocation<Link> baseLoc = base.getMeasureLocations().get(key);
				mergeMeasurementLocation(baseLoc, locToAdd, mode);
			}
		}
	}

	/**
	 * Merges the measurable data from one MeasurementLocation into another.
	 *
	 * <p>For each measurable (identified by type and mode) in the addition location:
	 * <ul>
	 *   <li>If the measurable does not exist in the base location, it is created and merged.
	 *   <li>If the measurable exists:
	 *       <ul>
	 *         <li>SKIP: Retains the base location's measurable.
	 *         <li>OVERRIDE: Replaces the base location's measurable with the new one.
	 *         <li>SUM: Sums the values from the addition measurable into the base measurable.
	 *       </ul>
	 * </ul>
	 *
	 * @param baseLoc The base MeasurementLocation that will be updated.
	 * @param addLoc  The MeasurementLocation that contains additional data.
	 * @param mode    The duplicate handling mode to apply.
	 */
	private static void mergeMeasurementLocation(MeasurementLocation<Link> baseLoc, MeasurementLocation<Link> addLoc, DuplicateHandling mode) {
		// Loop through each measurable identified by type and mode in the addition location.
		for (MeasurementLocation.TypeAndMode key : addLoc) {
			Measurable addMeasurable = addLoc.getMeasurableForMode(key.type(), key.mode());

			if (addMeasurable == null) {
				log.warn("No measurable found for type " + key.type() + " and mode " + key.mode() + " in location " + addLoc);
				continue;
			}

			if (!baseLoc.hasMeasurableForMode(key.type(), key.mode())) {
				// Create a new measurable in the base location and merge values.
				baseLoc.createMeasurable(key.type(), key.mode(), addMeasurable.getInterval());
				mergeMeasurable(baseLoc.getMeasurableForMode(key.type(), key.mode()), addMeasurable);
			} else {
				switch (mode) {
					case skip:
						// Retain the existing measurable; no action needed.
						break;
					case override:
						// Replace existing measurable with the addition measurable.
						baseLoc.createMeasurable(key.type(), key.mode(), addMeasurable.getInterval());
						mergeMeasurable(baseLoc.getMeasurableForMode(key.type(), key.mode()), addMeasurable);
						break;
					case error:
						throw new RuntimeException("The location " + baseLoc + " already contains a measurable for type " + key.type() + " and mode " + key.mode());
//					case SUM:
//						// Sum the values from both measurables.
//						Measurable baseMeasurable = baseLoc.getMeasurableForMode(key.type(), key.mode());
//						mergeMeasurable(baseMeasurable, addMeasurable);
//						break;
				}
			}
		}
	}

	/**
	 * Merges the time-series measurable values from the addition measurable into the base measurable.
	 *
	 * <p>This method iterates over each time-value entry in the addition measurable.
	 * If the base measurable already has a value for that time, the values are summed.
	 * Otherwise, the value from the addition measurable is used directly.
	 *
	 * @param baseMeasurable The Measurable object to update.
	 * @param addMeasurable  The Measurable object that provides additional values.
	 */
	private static void mergeMeasurable(Measurable baseMeasurable, Measurable addMeasurable) {
		for (Int2DoubleMap.Entry entry : addMeasurable.getValues().int2DoubleEntrySet()) {
			int time = entry.getIntKey();
			double value = entry.getDoubleValue();
			OptionalDouble existing = baseMeasurable.getAtSecond(time);
			if (existing.isPresent()) {
				double newVal = existing.getAsDouble() + value;
				baseMeasurable.setAtSecond(time, newVal);
			} else {
				baseMeasurable.setAtSecond(time, value);
			}
		}
	}

	/**
	 * Enum for specifying how duplicate locations should be handled during merge.
	 *
	 * <p>The available modes are:
	 * <ul>
	 *   <li>SKIP: Ignore the duplicate and keep the original measurable.
	 *   <li>OVERRIDE: Replace the original measurable with the duplicate.
	 *   <li>SUM: Sum the values of the measurables from both locations.
	 * </ul>
	 */
	public enum DuplicateHandling {
		error,
		skip,
		override
	}
}
