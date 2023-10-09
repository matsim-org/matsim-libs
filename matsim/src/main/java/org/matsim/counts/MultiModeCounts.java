package org.matsim.counts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * This class provides count object, that can assign any measurable values (traffic volumes, velocities e.g.) for any matsim transport mode
 * to an identifiable object (links, nodes, transit stations e.g)
 * Structure is similar to regular counts, but more flexible to use.
 * */
public final class MultiModeCounts<T extends Identifiable<T>> implements Attributable {

	public static final String ELEMENT_NAME = "counts";

	// TODO: merge into Counts class to have same API

	private String name;
	private String description;
	private String source;

	private int year;

	private final Map<Id<T>, MeasurementLocation<T>> locations = new TreeMap<>();

	private final Attributes attributes = new AttributesImpl();

	public MultiModeCounts(){}

	/**
	 * Creates a MeasurementLocation object and adds to count tree map. Argument has to be an id for an matsim Identifiable object (link, node, pt station e.g).
	 * */
	public MeasurementLocation<T> createAndAddCount(final Id<T> id, String stationName){

		if (this.locations.containsKey(id)) {
			throw new RuntimeException("There is already a measurement object for location " + id.toString());
		}

		MeasurementLocation<T> count = new MeasurementLocation<T>(id, stationName);
		this.locations.put(id, count);

		return count;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getSource() {
		return source;
	}

	public int getYear() {
		return year;
	}

	public Set<String> getMeasurableTags() {
		return locations.values().stream()
			.map(MeasurementLocation::getMeasurables)
			.flatMap(m -> m.keySet().stream())
			.collect(Collectors.toSet());
	}

	public Map<Id<T>, MeasurementLocation<T>> getMeasureLocations() {
		return locations;
	}

	public MeasurementLocation<T> getMeasureLocation(Id<T> id) {
		return this.locations.get(id);
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		return "[name=" + this.name + "]" + "[nof_locations=" + this.locations.size() + "]";
	}
}
