package org.matsim.contribs.discrete_mode_choice.modules.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Config group for DefaultModeAvailability and CarModeAvailability.
 * 
 * @author sebhoerl
 *
 */
public class ModeAvailabilityConfigGroup extends ComponentConfigGroup {
	private Collection<String> availableModes = new HashSet<>(Arrays.asList("car", "bike", "pt", "walk"));

	public static final String AVAILABLE_MODES = "availableModes";
	public static final String AVAILABLE_MODES_CMT = "Defines which modes are avialable to the agents.";

	public ModeAvailabilityConfigGroup(String componentType, String componentName) {
		super(componentType, componentName);
	}

	/**
	 * @param availableModes -- {@value AVAILABLE_MODES_CMT}
	 */
	public void setAvailableModes(Collection<String> availableModes) {
		this.availableModes = new HashSet<>(availableModes);
	}

	public Collection<String> getAvailableModes() {
		return availableModes;
	}

	@StringSetter(AVAILABLE_MODES)
	public void setAvailableModesAsString(String constrainedModes) {
		this.availableModes = Arrays.asList(constrainedModes.split(",")).stream().map(String::trim)
				.collect(Collectors.toSet());
	}

	@StringGetter(AVAILABLE_MODES)
	public String getAvailableModesAsString() {
		return String.join(", ", availableModes);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();

		comments.put(AVAILABLE_MODES, AVAILABLE_MODES_CMT );

		return comments;
	}
}
