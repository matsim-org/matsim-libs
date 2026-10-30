package org.matsim.application.analysis.population;

import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

/**
 * Conversion parameters of the scenario's scoring model. The default reads the run's injected
 * person parameters, including its income and subpopulation treatment. It does not normalize
 * incomes again. Bind a scenario-specific implementation if a custom scorer uses different
 * coefficients; return NaN for an unsupported conversion to retain MUSE without monetary values.
 */
@ImplementedBy(PersonScoringVttsParameters.class)
public interface VttsParameters {

	/** Score per money unit for this person; nonpositive or nonfinite values prevent monetization. */
	double marginalUtilityOfMoney(Person person);

	/**
	 * Direct traveling coefficient in score/hour; excludes PT waiting and access time.
	 * Return NaN when this mode has no supported linear conversion, rather than substituting zero.
	 */
	double marginalUtilityOfTraveling(Person person, String mode);
}

final class PersonScoringVttsParameters implements VttsParameters {

	private final ScoringParametersForPerson parameters;

	@Inject
	PersonScoringVttsParameters(ScoringParametersForPerson parameters) {
		this.parameters = parameters;
	}

	@Override
	public double marginalUtilityOfMoney(Person person) {
		// The injected provider resolves the person's subpopulation and any income treatment
		// installed by the scenario. Reading config defaults here would bypass that resolution.
		return parameters.getScoringParameters(person).marginalUtilityOfMoney;
	}

	@Override
	public double marginalUtilityOfTraveling(Person person, String mode) {
		var modeParameters = parameters.getScoringParameters(person).modeParams.get(mode);
		// Core scoring stores this coefficient per second; MUSE and the exports use per hour.
		return modeParameters == null ? Double.NaN : 3600 * modeParameters.marginalUtilityOfTraveling_s;
	}
}
