package org.matsim.counts;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class MultiModeCounts implements Attributable {

	public static final String ELEMENT_NAME = "multiModeCounts";

	public final Class<? extends Identifiable> identifiable;

	private final Set<String> measurableTags = new HashSet<>();

	private String name;
	private String description;
	private String source;

	private int year;

	private final TreeMap<Id<? extends Identifiable>, MultiModeCount> counts = new TreeMap<>();

	private final Attributes attributes = new AttributesImpl();

	public MultiModeCounts(String name, int year){
		this(name, year, Link.class);
	}

	public MultiModeCounts(String name, int year, Class<? extends Identifiable> identifiable){
		this.year = year;
		this.name = name;
		this.identifiable = identifiable;
	}

	/**
	 * Creates a MultiModeCount object and adds to count tree map. Argument has to be an id for an matsim Identifiable object (link, node, pt station e.g).
	 * A year can be passed as argument if count sources from different years are used, if not the year of the counts collection will be handed over.
	 * */
	public MultiModeCount createAndAddCount(final Id<? extends Identifiable> id, String stationName, @Nullable Integer year){

		if (this.counts.containsKey(id)) {
			throw new RuntimeException("There is already a counts object for location " + id.toString());
		}

		MultiModeCount count = year == null ? new MultiModeCount(id, stationName, this.year, measurableTags): new MultiModeCount(id, stationName, year, measurableTags);
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

	public TreeMap<Id<? extends Identifiable>, MultiModeCount> getCounts() {
		return counts;
	}

	public Set<String> getMeasurableTags() {
		return measurableTags;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public final String toString() {
		return "[name=" + this.name + "]" + "[nof_counts=" + this.counts.size() + "]";
	}
}
