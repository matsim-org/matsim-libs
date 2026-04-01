package org.matsim.smallScaleCommercialTrafficGeneration;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Delegates scoring function creation to different factories based on a person's subpopulation.
 */
public class SubpopulationDelegatingScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringFunctionFactory defaultFactory;
	private final Map<String, ScoringFunctionFactory> factoriesBySubpopulation;

	@Inject SubpopulationDelegatingScoringFunctionFactory( CharyparNagelScoringFunctionFactory defaultFactory,
														   Map<String, ScoringFunctionFactory> factoriesBySubpopulation ) {
		this.defaultFactory = Objects.requireNonNull(defaultFactory, "defaultFactory must not be null");
		Objects.requireNonNull(factoriesBySubpopulation, "factoriesBySubpopulation must not be null");

		Map<String, ScoringFunctionFactory> copy = new LinkedHashMap<>();
		factoriesBySubpopulation.forEach((subpopulation, factory) -> {
			if (subpopulation == null) {
				throw new IllegalArgumentException("Subpopulation keys must not be null. Use the default factory instead.");
			}
			copy.put(subpopulation, Objects.requireNonNull(factory, "factory must not be null"));
		});
		this.factoriesBySubpopulation = Map.copyOf(copy);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		String subpopulation = PopulationUtils.getSubpopulation(person);
		ScoringFunctionFactory factory = subpopulation == null ? defaultFactory : factoriesBySubpopulation.getOrDefault(subpopulation, defaultFactory);
		return factory.createNewScoringFunction(person);
	}
}
