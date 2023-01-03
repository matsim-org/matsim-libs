package org.matsim.contribs.discrete_mode_choice.modules.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Config group for VehicleTripConstriant.
 * 
 * @author sebhoerl
 *
 */
public class VehicleTripConstraintConfigGroup extends ComponentConfigGroup {
	private Collection<String> restrictedModes = new HashSet<>(Arrays.asList("car", "bike"));
	private boolean isAdvanced = true;

	private static final String RESTRICTED_MODES = "restrictedModes";
	private static final String IS_ADVANCED = "isAdvanced";

	public VehicleTripConstraintConfigGroup(String componentType, String componentName) {
		super(componentType, componentName);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();

		comments.put(RESTRICTED_MODES,
				"Defines which modes must fulfill continuity constraints (can only be used where they have been brough to before)");

		comments.put(IS_ADVANCED, "Defines if the advanced constriant is used (vehicles must be brought back home).");

		return comments;
	}

	public void setRestrictedModes(Collection<String> restrictedModes) {
		this.restrictedModes = new HashSet<>(restrictedModes);
	}

	@StringSetter(RESTRICTED_MODES)
	public void setRestrictedModesAsString(String restrictedModes) {
		this.restrictedModes = new HashSet<>(
				Arrays.asList(restrictedModes.split(",")).stream().map(String::trim).collect(Collectors.toSet()));
	}

	public Collection<String> getRestrictedModes() {
		return restrictedModes;
	}

	@StringGetter(RESTRICTED_MODES)
	public String getRestrictedModesAsString() {
		return String.join(", ", restrictedModes);
	}

	@StringGetter(IS_ADVANCED)
	public boolean getIsAdvanced() {
		return isAdvanced;
	}

	@StringSetter(IS_ADVANCED)
	public void setIsAdvanced(boolean isAdvanced) {
		this.isAdvanced = isAdvanced;
	}
}
