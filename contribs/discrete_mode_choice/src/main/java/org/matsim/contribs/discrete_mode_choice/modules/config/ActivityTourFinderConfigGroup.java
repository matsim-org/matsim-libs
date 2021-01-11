package org.matsim.contribs.discrete_mode_choice.modules.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuration for the ActivityTourFinder.
 * 
 * @author sebhoerl
 */
public class ActivityTourFinderConfigGroup extends ComponentConfigGroup {
	private Collection<String> activityTypes = Arrays.asList("home");

	public static final String ACTIVITY_TYPES = "activityTypes";

	public ActivityTourFinderConfigGroup(String componentType, String componentName) {
		super(componentType, componentName);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();

		comments.put(ACTIVITY_TYPES,
				"Comma-separated activity types which should be considered as start and end of a tour. If a plan does not start or end with such an activity additional tours are added.");

		return comments;
	}

	@StringGetter(ACTIVITY_TYPES)
	public String getActivityTypesAsString() {
		return String.join(", ", activityTypes);
	}

	@StringSetter(ACTIVITY_TYPES)
	public void setActivityTypesAsString(String activityTypes) {
		this.activityTypes = Arrays.asList(activityTypes.split(",")).stream().map(String::trim)
				.collect(Collectors.toSet());
	}

	public Collection<String> getActivityTypes() {
		return activityTypes;
	}

	public void setActivityTypes(Collection<String> activityTypes) {
		this.activityTypes = activityTypes;
	}
}
