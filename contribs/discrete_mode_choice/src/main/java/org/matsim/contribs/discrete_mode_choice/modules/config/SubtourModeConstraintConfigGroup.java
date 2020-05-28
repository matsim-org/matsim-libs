package org.matsim.contribs.discrete_mode_choice.modules.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Config group for the SubtourModeConstraint.
 * 
 * @author sebhoerl
 *
 */
public class SubtourModeConstraintConfigGroup extends ComponentConfigGroup {
	private Collection<String> constrainedModes = new HashSet<>();

	public final static String CONSTRAINED_MODES = "constrainedModes";

	public SubtourModeConstraintConfigGroup(String componentType, String componentName) {
		super(componentType, componentName);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();

		comments.put(CONSTRAINED_MODES,
				"Modes for which the sub-tour behaviour should be replicated. If all available modes are put here, this equals to SubTourModeChoice with singleLegProbability == 0.0; if only the constrained modes are put here, it equals singleLegProbability > 0.0");

		return comments;
	}

	public void setConstrainedModes(Collection<String> contrainedModes) {
		this.constrainedModes = new HashSet<>(constrainedModes);
	}

	public Collection<String> getConstrainedModes() {
		return constrainedModes;
	}

	@StringSetter(CONSTRAINED_MODES)
	public void setConstrainedModesAsString(String constrainedModes) {
		this.constrainedModes = Arrays.asList(constrainedModes.split(",")).stream().map(String::trim)
				.collect(Collectors.toSet());
	}

	@StringGetter(CONSTRAINED_MODES)
	public String getConstrainedModesAsString() {
		return String.join(", ", constrainedModes);
	}
}
