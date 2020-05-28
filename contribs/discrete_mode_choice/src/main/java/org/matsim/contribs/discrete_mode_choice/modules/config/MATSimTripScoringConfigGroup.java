package org.matsim.contribs.discrete_mode_choice.modules.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Config group for the MATSimTripScoringEstimator.
 * 
 * @author sebhoerl
 *
 */
public class MATSimTripScoringConfigGroup extends ComponentConfigGroup {
	private Collection<String> ptLegModes = new HashSet<>(Arrays.asList("pt"));

	public final static String PT_LEG_MODES = "ptLegModes";

	public MATSimTripScoringConfigGroup(String componentType, String componentName) {
		super(componentType, componentName);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();

		comments.put(PT_LEG_MODES,
				"Modes which are considered as public transit, i.e. they involve waiting for a vehicle.");

		return comments;
	}

	public void setPtLegModes(Collection<String> ptLegModes) {
		this.ptLegModes = new HashSet<>(ptLegModes);
	}

	public Collection<String> getPtLegModes() {
		return ptLegModes;
	}

	@StringSetter(PT_LEG_MODES)
	public void setPtLegModesAsString(String ptLegModes) {
		this.ptLegModes = Arrays.asList(ptLegModes.split(",")).stream().map(String::trim).collect(Collectors.toSet());
	}

	@StringGetter(PT_LEG_MODES)
	public String getPtLegModesAsString() {
		return String.join(", ", ptLegModes);
	}
}
