package org.matsim.counts;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MultiModeCount<T> implements Identifiable<T>, Attributable {

	public static final String ELEMENT_NAME = "multiModeCount";
	private final Id<T> id;
	private final Map<String, Volumes> volumes = new HashMap<>();
	private final Set<String> modes = new HashSet<>();

	private final Set<String> dailyVolumeModes = new HashSet<>();

	private String stationName;
	private String description;
	private int year;

	@Inject
	private Attributes attributes;

	MultiModeCount(final Id<T> id, String stationName, int year){
		this.id = id;
		this.stationName = stationName;
		this.year = year;
	}

	@Override
	public Id<T> getId() {

		return id;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	public Volumes createVolume(String mode, boolean hasOnlyDailyVolumes){
		Volumes volumes = new Volumes(mode, hasOnlyDailyVolumes);
		this.volumes.put(mode, volumes);

		if(hasOnlyDailyVolumes)
			dailyVolumeModes.add(mode);

		return volumes;
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

	public Volumes getVolumesForMode(String mode) {
		return this.volumes.get(mode);
	}

	public Set<String> getDailyVolumeModes() {
		return dailyVolumeModes;
	}

	@Override
	public String toString() {
		return "MultiModeCount{" +
				"id=" + id +
				", volumes=" + volumes +
				", modes=" + modes +
				", stationName='" + stationName + '\'' +
				", description='" + description + '\'' +
				", year=" + year +
				", attributes=" + attributes +
				'}';
	}
}
