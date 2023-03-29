package org.matsim.contribs.discrete_mode_choice.modules.config;

import org.matsim.contribs.discrete_mode_choice.components.constraints.VehicleTourConstraint;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Config group for {@link VehicleTourConstraint}.
 * 
 * @author sebhoerl
 *
 */
public class VehicleTourConstraintConfigGroup extends ComponentConfigGroup {
	private Collection<String> restrictedModes = new HashSet<>(Arrays.asList("car", "bike"));

	private static final String RESTRICTED_MODES = "restrictedModes";
	public static final String RESTRICTED_MODES_CMT = "Defines which modes must fulfill continuity constraints (can only be used where they have been brough to before)";

	public VehicleTourConstraintConfigGroup(String componentType, String componentName) {
		super(componentType, componentName);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();
		comments.put(RESTRICTED_MODES, RESTRICTED_MODES_CMT );
		return comments;
	}

	/**
	 * @param restrictedModes -- {@value RESTRICTED_MODES_CMT}
	 */
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
}
