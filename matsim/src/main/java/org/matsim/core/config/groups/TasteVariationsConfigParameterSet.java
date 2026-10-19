package org.matsim.core.config.groups;

import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.scoring.functions.ModeUtilityParameters;

import java.util.Set;

/**
 * Config group for taste variations, i.e. individual utility parameters for specific persons.
 * See
 */
public final class TasteVariationsConfigParameterSet extends ReflectiveConfigGroup {

	// Other than AdvancedScoring (currently in the matsim-berlin repo),this here does NOT need a switch of type
	// "isUsingTasteVariations".  Since it is simply always there, but the default settings are "incomeExponent=0" (i.e. no
	// income dependency) and "variationsOf=emptySet" (i.e. no taste variations).

	/**
	 * The name of the config group.
	 */
	public static final String SET_TYPE = "tasteVariations";

	@Parameter
	@PositiveOrZero
	@Comment("Exponent for income dependent scoring. Exponent for (average_income / personal_income) ** x. Default is x=0, which disables it.")
	private double incomeExponent = 0;

	@Parameter
	@Comment("List of utility parameters that are loaded from each person.")
	private Set<ModeUtilityParameters.Type> variationsOf = Set.of();

	@Parameter
	@Comment("List of subpopulations for which no variations will be applied.")
	private Set<String> excludeSubpopulations = Set.of();

	public TasteVariationsConfigParameterSet() {
		super(SET_TYPE);
	}

	public double getIncomeExponent() {
		return incomeExponent;
	}

	public void setIncomeExponent(double incomeExponent) {
		this.incomeExponent = incomeExponent;
	}

	public Set<ModeUtilityParameters.Type> getVariationsOf() {
		return variationsOf;
	}

	public void setVariationsOf(Set<ModeUtilityParameters.Type> variationsOf) {
		this.variationsOf = variationsOf;
	}

	public Set<String> getExcludeSubpopulations() {
		return excludeSubpopulations;
	}

	public void setExcludeSubpopulations(Set<String> excludeSubpopulations) {
		this.excludeSubpopulations = excludeSubpopulations;
	}

	/**
	 * Specifies a distribution of how the utility parameters are varied.
	 * Note that this enum is now used in other places and not directly in the config.
	 */
	public enum VariationType {
		fixed, normal, truncatedNormal, gumbel
	}

}
