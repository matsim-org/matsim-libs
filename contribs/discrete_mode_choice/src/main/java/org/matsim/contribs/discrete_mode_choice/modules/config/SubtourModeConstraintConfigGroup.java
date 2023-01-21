package org.matsim.contribs.discrete_mode_choice.modules.config;

import org.matsim.contribs.discrete_mode_choice.components.constraints.SubtourModeConstraint;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Config group for the {@link SubtourModeConstraint}.
 * 
 * @author sebhoerl
 *
 */
public class SubtourModeConstraintConfigGroup extends ComponentConfigGroup {
	private Collection<String> constrainedModes = new HashSet<>();

	public final static String CONSTRAINED_MODES = "constrainedModes";
	public final static String CONSTRAINED_MODES_CMT = "Modes for which the sub-tour behaviour should be replicated. If all available modes are put here, this equals to SubTourModeChoice with singleLegProbability == 0.0; if only the constrained modes are put here, it equals singleLegProbability > 0.0";

	public SubtourModeConstraintConfigGroup(String componentType, String componentName) {
		super(componentType, componentName);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();

		comments.put(CONSTRAINED_MODES, CONSTRAINED_MODES_CMT );

		return comments;
	}

	/**
	 * @param constrainedModes -- {@value CONSTRAINED_MODES_CMT}
	 */
	public void setConstrainedModes(Collection<String> constrainedModes) {
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
