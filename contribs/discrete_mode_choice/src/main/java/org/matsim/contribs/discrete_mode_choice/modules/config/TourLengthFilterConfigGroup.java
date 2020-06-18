package org.matsim.contribs.discrete_mode_choice.modules.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Config group for the TourLengthFilter.
 * 
 * @author sebhoerl
 *
 */
public class TourLengthFilterConfigGroup extends ComponentConfigGroup {
	private int maximumLength = 10;

	public static final String MAXIMUM_LENGTH = "maximumLength";

	public TourLengthFilterConfigGroup(String componentType, String componentName) {
		super(componentType, componentName);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();

		comments.put(MAXIMUM_LENGTH, "Defines the maximum allowed length of a tour.");

		return comments;
	}

	@StringSetter(MAXIMUM_LENGTH)
	public void setMaximumLength(int maximumLength) {
		this.maximumLength = maximumLength;
	}

	@StringGetter(MAXIMUM_LENGTH)
	public int getMaximumLength() {
		return maximumLength;
	}
}
