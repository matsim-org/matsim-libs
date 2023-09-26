package org.matsim.counts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A MultiModeCount can hold measurable traffic stats (traffic volumes or velocities e.g.) for a single matsim infrastructure object.
 * Measurable values are provided as Measurable instances for a certain mode. It is possible to assign the same kind of value to
 * several transport modes.
 * A single MultiModeCount instance for example can hold traffic volumes for the mode 'car' and average velocities for the mode 'freight'.
 */
public final class MultiModeCount<T> implements Identifiable<T>, Attributable {

	public static final String ELEMENT_NAME = "multiModeCount";
	private final Id<T> id;
	private final Set<String> modes = new HashSet<>();

	private final Map<String, Map<String, Measurable>> measurables = new HashMap<>();

	private final Set<String> dailyVolumeModes = new HashSet<>();

	private String stationName;
	private String description;
	private int year;
	private final Set<String> measurableTags;

	private final Attributes attributes = new AttributesImpl();

	MultiModeCount(final Id<T> id, String stationName, int year, Set<String> measurableTags) {
		this.id = id;
		this.stationName = stationName;
		this.year = year;
		this.measurableTags = measurableTags;
	}

	public Id<T> getId() {

		return id;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	public Measurable addMeasurable(String typeOfMeasurableData, String mode, int interval) {
		this.measurableTags.add(typeOfMeasurableData);
		Measurable measurable = new Measurable(mode, typeOfMeasurableData, interval);

		if (!this.measurables.containsKey(typeOfMeasurableData))
			this.measurables.put(typeOfMeasurableData, new HashMap<>());

		this.measurables.get(typeOfMeasurableData).putIfAbsent(mode, measurable);

		return measurable;
	}

	public Measurable createVolume() {
		return addMeasurable(Measurable.VOLUMES, TransportMode.car, 60);
	}

	public Measurable createVolume(String mode) {
		return addMeasurable(Measurable.VOLUMES, mode, 60);
	}

	public Measurable createVelocity(String mode) {
		return addMeasurable(Measurable.VELOCITIES, mode, 60);
	}

	public Measurable createPassenger(String mode) {
		return addMeasurable(Measurable.PASSENGERS, mode, 60);
	}

	public void setStationName(String stationName) {
		this.stationName = stationName;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public Set<String> getModes() {
		return modes;
	}

	public String getDescription() {
		return description;
	}

	public int getYear() {
		return year;
	}

	public String getStationName() {
		return stationName;
	}

	public Measurable getVolumesForMode(String mode) {
		return this.measurables.get(Measurable.VOLUMES).get(mode);
	}

	Map<String, Map<String, Measurable>> getMeasurables() {
		return this.measurables;
	}

	public Set<String> getDailyVolumeModes() {
		return dailyVolumeModes;
	}

	public Measurable getMeasurable(String measurableType, String mode) {
		return this.measurables.get(measurableType).get(mode);
	}

	@Override
	public String toString() {
		return "MultiModeCount{" +
				"id=" + id +
				", modes=" + modes +
				", stationName='" + stationName + '\'' +
				", year=" + year +
				", measurableTags=" + measurableTags +
				'}';
	}
}
