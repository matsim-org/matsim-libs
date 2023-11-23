package org.matsim.contribs.discrete_mode_choice.modules.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private final Logger log = LogManager.getLogger(SubtourModeConstraintConfigGroup.class );
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
		log.warn( "setContrainedModes had a typo in its implementation and in consequence ignored its arguments.  This has now been corrected, but presumably changes one or " +
					  "the other result.  You need comment out your setting of this if you want to obtain your old results.  They will, however, not be consistent with what you intended.  kai, jan'23");
		this.constrainedModes = new HashSet<>(constrainedModes);
	}
	// What I found is in comments below; note the typo in contrainedModes.  I think that (only) the above version is correct.  This fails the sioux falls test; changing it therefore.  kai, jan'23
//	public void setConstrainedModes(Collection<String> contrainedModes) {
//		this.constrainedModes = new HashSet<>(constrainedModes);
//	}

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
