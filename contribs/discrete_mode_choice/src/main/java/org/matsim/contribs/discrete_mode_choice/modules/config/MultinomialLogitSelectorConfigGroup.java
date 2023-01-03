package org.matsim.contribs.discrete_mode_choice.modules.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Config group for the MultinomialLogitSelector
 * 
 * @author sebhoerl
 *
 */
public class MultinomialLogitSelectorConfigGroup extends ComponentConfigGroup {
	private double minimumUtility = -700.0;
	private double maximumUtility = 700.0;
	private boolean considerMinimumUtility = false;

	public static final String MINIMUM_UTILITY = "minimumUtility";
	public static final String MAXIMUM_UTILITY = "maximumUtility";
	public static final String CONSIDER_MINIMUM_UTILITY = "considerMinimumUtility";

	public MultinomialLogitSelectorConfigGroup(String componentType, String componentName) {
		super(componentType, componentName);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();

		comments.put(MINIMUM_UTILITY,
				"Candidates with a utility lower than that threshold will not be considered by default.");
		comments.put(MAXIMUM_UTILITY, "Candidates with a utility above that threshold will be cut off to this value.");
		comments.put(CONSIDER_MINIMUM_UTILITY,
				"Defines whether candidates with a utility lower than the minimum utility should be filtered out.");

		return comments;
	}

	@StringSetter(MINIMUM_UTILITY)
	public void setMinimumUtility(double minimumUtility) {
		this.minimumUtility = minimumUtility;
	}

	@StringGetter(MINIMUM_UTILITY)
	public double getMinimumUtility() {
		return minimumUtility;
	}

	@StringSetter(MAXIMUM_UTILITY)
	public void setMaximumUtility(double maximumUtility) {
		this.maximumUtility = maximumUtility;
	}

	@StringGetter(MAXIMUM_UTILITY)
	public double getMaximumUtility() {
		return maximumUtility;
	}

	@StringSetter(CONSIDER_MINIMUM_UTILITY)
	public void setConsiderMinimumUtility(boolean considerMinimumUtility) {
		this.considerMinimumUtility = considerMinimumUtility;
	}

	@StringGetter(CONSIDER_MINIMUM_UTILITY)
	public boolean getConsiderMinimumUtility() {
		return considerMinimumUtility;
	}
}
