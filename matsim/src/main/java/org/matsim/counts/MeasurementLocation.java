package org.matsim.counts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import jakarta.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A MeasurementLocation can hold measurable traffic stats (traffic volumes or velocities e.g.) for a single matsim infrastructure object.
 * Measurable values are provided as Measurable instances for a certain mode. It is possible to assign the same kind of value to
 * several transport modes.
 * A single MeasurementLocation instance for example can hold traffic volumes for the mode 'car' and average velocities for the mode 'freight'.
 */
public final class MeasurementLocation<T> implements Attributable, Iterable<MeasurementLocation.TypeAndMode> {

	static final String ELEMENT_NAME = "location";

	private final Id<T> refId;
	private final Map<TypeAndMode, Measurable> measurables = new LinkedHashMap<>();
	private final Attributes attributes = new AttributesImpl();

	private String id;
	private String stationName;
	private String description;
	private Coord coordinates;

	MeasurementLocation(final Id<T> refId, String stationName) {
		this.refId = refId;
		this.stationName = stationName;
	}

	/**
	 * Id reference to the matsim infrastructure object.
	 */
	public Id<T> getRefId() {
		return refId;
	}

	/**
	 * Id that may be used internally, not corresponding to matsim ids.
	 */
	public String getId() {
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
		return measurables.computeIfAbsent(new TypeAndMode(typeOfMeasurableData, mode),
			k -> new Measurable(mode, typeOfMeasurableData, interval));
	}

	/**
	 * Delete measurable for certain mode.
	 */
	public boolean deleteMeasurable(String typeOfMeasurableData, String mode) {
		return this.measurables.remove(new TypeAndMode(typeOfMeasurableData, mode)) != null;
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

	/**
	 * Returns the display name of this location. If the station name is set, it is returned. Otherwise the id is returned.
	 */
	public String getDisplayName() {
		if (stationName != null)
			return stationName;
		if (id != null)
			return id;

		return description;
	}

	public void setId(String id) {
		this.id = id;
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
		return this.measurables.get(new TypeAndMode(Measurable.VOLUMES, mode));
	}

	/**
	 * Return whether this location has measurable data for certain mode.
	 */
	public boolean hasMeasurableForMode(String measurableType, String mode) {
		return this.measurables.containsKey(new TypeAndMode(measurableType, mode));
	}

	@Nullable
	public Measurable getMeasurableForMode(String measurableType, String mode) {
		return this.measurables.get(new TypeAndMode(measurableType, mode));
	}

	Map<TypeAndMode, Measurable> getMeasurables() {
		return this.measurables;
	}

	@Override
	public String toString() {
		return "MeasurementLocation{" +
			"id=" + refId +
			", stationName='" + stationName + '\'' +
			'}';
	}

	@Override
	public Iterator<TypeAndMode> iterator() {
		return this.measurables.keySet().iterator();
	}

	/**
	 * Stores the measurable type and mode of a {@link Measurable}.
	 */
	public final record TypeAndMode(String type, String mode) {
	}

}
