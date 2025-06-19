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

	/**
	 * The name of the config group.
	 */
	public static final String SET_TYPE = "tasteVariations";

	@Parameter
	@PositiveOrZero
	@Comment("Exponent for income dependent scoring. Exponent for (global_income / personal_income) ** x. Default is 0, which disables it.")
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
