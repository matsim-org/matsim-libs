package org.matsim.counts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * This class provides count object, that can assign any measurable values (traffic volumes, velocities e.g.) for any matsim transport mode
 * to an identifiable object (links, nodes, transit stations e.g)
 * Structure is similar to regular counts, but more flexible to use.
 */
public final class Counts<T extends Identifiable<T>> implements Attributable, MatsimToplevelContainer {

	public static final String ELEMENT_NAME = "counts";
	private final Map<Id<T>, MeasurementLocation<T>> locations = new TreeMap<>();
	private final Attributes attributes = new AttributesImpl();
	private String name;
	private String description;
	private String source;
	private int year;

	public Counts() {
	}

	/**
	 * Creates a MeasurementLocation object and adds to count tree map. Argument has to be an id for an matsim Identifiable object (link, node, pt station e.g).
	 */
	public MeasurementLocation<T> createAndAddMeasureLocation(final Id<T> id, String stationName) {

		if (this.locations.containsKey(id)) {
			throw new RuntimeException("There is already a measurement object for location " + id.toString());
		}

		MeasurementLocation<T> loc = new MeasurementLocation<T>(id, stationName);
		this.locations.put(id, loc);

		return loc;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	/**
	 * Returns all measured types of observations for all modes.
	 */
	public Set<String> getMeasurableTypes() {
		return locations.values().stream()
			.map(MeasurementLocation::getMeasurables)
			.flatMap(e -> e.keySet().stream())
			.map(MeasurementLocation.TypeAndMode::type)
			.collect(Collectors.toSet());
	}

	public Map<Id<T>, MeasurementLocation<T>> getMeasureLocations() {
		return locations;
	}

	public MeasurementLocation<T> getMeasureLocation(Id<T> id) {
		return this.locations.get(id);
	}

	/// Old API
	// These functions belong to the older Counts API and are kept for compatibility
	// Consider using the new API, which provides more flexibility (createAndAddLocation, getMeasureLocations)

	/**
	 * Old style API to create a measurement for car volumes at one station.
	 * Consider using {@link #createAndAddMeasureLocation(Id, String)} instead.
	 *
	 * @param linkId      the link to which the counting station is assigned, must be unique
	 * @param stationName some additional identifier for humans, e.g. the original name/id of the counting station
	 * @return the created Count object, or {@linkplain RuntimeException} if it could not be created because it already exists
	 * @deprecated use {@link #createAndAddMeasureLocation(Id, String)} instead
	 */
	@Deprecated
	public final Count<T> createAndAddCount(final Id<T> linkId, final String stationName) {
		MeasurementLocation<T> location = createAndAddMeasureLocation(linkId, stationName);
		return new Count<>(location);
	}

	/**
	 * Retrieve map of all counts. This will be inefficient because all intermediate objects for the old API will be created.
	 *
	 * @deprecated use {@link #getMeasureLocations()} instead
	 */
	@Deprecated
	public final TreeMap<Id<T>, Count<T>> getCounts() {
		TreeMap<Id<T>, Count<T>> result = new TreeMap<>();

		for (Map.Entry<Id<T>, MeasurementLocation<T>> e : locations.entrySet()) {
			result.put(e.getKey(), new Count<>(e.getValue()));
		}

		return result;
	}

	/**
	 * Use {@link #getMeasureLocation(Id)} instead.
	 */
	@Deprecated
	public Count<T> getCount(final Id<T> locId) {
		MeasurementLocation<T> loc = getMeasureLocation(locId);
		return loc == null ? null : new Count<>(loc);
	}


	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		return "[name=" + this.name + "]" + "[nof_locations=" + this.locations.size() + "]";
	}

	@Override public MatsimFactory getFactory(){
		throw new RuntimeException( "not implemented" );
		// yy I need this to fulfill MatsimToplevelContainer.  Which I need to be able to use ProjectionUtils.putCRS(...) in the readers/writers.  kai, feb'24
	}
}
