package org.matsim.counts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A MeasurementLocation can hold measurable traffic stats (traffic volumes or velocities e.g.) for a single matsim infrastructure object.
 * Measurable values are provided as Measurable instances for a certain mode. It is possible to assign the same kind of value to
 * several transport modes.
 * A single MeasurementLocation instance for example can hold traffic volumes for the mode 'car' and average velocities for the mode 'freight'.
 */
public final class MeasurementLocation<T> implements Identifiable<T>, Attributable {

	static final String ELEMENT_NAME = "location";

	private final Id<T> id;
	private final Map<String, Map<String, Measurable>> measurables = new HashMap<>();
	private final Attributes attributes = new AttributesImpl();
	private String stationName;
	private String description;
	private Coord coordinates;

	MeasurementLocation(final Id<T> id, String stationName) {
		this.id = id;
		this.stationName = stationName;
	}

	public Id<T> getId() {
		return id;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	/**
	 * Create arbitrary measurable for certain mode and minute interval. If this measurable exists already, it is returned.
	 */
	public Measurable createMeasurable(String typeOfMeasurableData, String mode, int interval) {
		Map<String, Measurable> map = this.measurables.computeIfAbsent(typeOfMeasurableData, k -> new LinkedHashMap<>());
		return map.computeIfAbsent(mode, k -> new Measurable(mode, typeOfMeasurableData, interval));
	}

	/**
	 * Delete measurable for certain mode.
	 */
	public boolean deleteMeasurable(String typeOfMeasurableData, String mode) {
		if (this.measurables.containsKey(typeOfMeasurableData)) {
			return this.measurables.get(typeOfMeasurableData).remove(mode) != null;
		}

		return false;
	}

	/**
	 * Create hourly volumes for car mode.
	 */
	public Measurable createVolume() {
		return createMeasurable(Measurable.VOLUMES, TransportMode.car, Measurable.HOURLY);
	}

	/**
	 * Create hourly values for certain mode.
	 */
	public Measurable createVolume(String mode) {
		return createMeasurable(Measurable.VOLUMES, mode, Measurable.HOURLY);
	}

	public Measurable createVolume(String mode, int interval) {
		return createMeasurable(Measurable.VOLUMES, mode, interval);
	}

	public Measurable createVelocity(String mode, int interval) {
		return createMeasurable(Measurable.VELOCITIES, mode, interval);
	}

	public Measurable createPassengerCounts(String mode, int interval) {
		return createMeasurable(Measurable.PASSENGERS, mode, interval);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getStationName() {
		return stationName;
	}

	public void setStationName(String stationName) {
		this.stationName = stationName;
	}

	public Coord getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(Coord coordinates) {
		this.coordinates = coordinates;
	}

	public Measurable getVolumesForMode(String mode) {
		return this.measurables.get(Measurable.VOLUMES).get(mode);
	}

	@Nullable
	public Measurable getMeasurableForMode(String measurableType, String mode) {
		return this.measurables.get(measurableType).get(mode);
	}

	Map<String, Map<String, Measurable>> getMeasurables() {
		return this.measurables;
	}

	@Override
	public String toString() {
		return "MeasurementLocation{" +
			"id=" + id +
			", stationName='" + stationName + '\'' +
			'}';
	}
}
