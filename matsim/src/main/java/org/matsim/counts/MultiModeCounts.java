package org.matsim.counts;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

import javax.annotation.Nullable;
import java.util.TreeMap;

public class MultiModeCounts<T> implements Attributable {

	public static final String ELEMENT_NAME = "multi_mode_counts";

	private String name;
	private String description;
	private String source;

	private int year;

	private final TreeMap<Id<T>, MultiModeCount<T>> counts = new TreeMap<>();

	@Inject
	private Attributes attributes;

	public MultiModeCounts(){

	}

	public MultiModeCount<T> createAndAddCount(final Id<T> id, String stationName, @Nullable Integer year){

		MultiModeCount<T> count = year == null ? new MultiModeCount<>(id, stationName, this.year): new MultiModeCount<>(id, stationName, year);
		this.counts.put(id, count);

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

	public TreeMap<Id<T>, MultiModeCount<T>> getCounts() {
		return counts;
	}

	@Override
	public Attributes getAttributes() {
		return null;
	}
}
