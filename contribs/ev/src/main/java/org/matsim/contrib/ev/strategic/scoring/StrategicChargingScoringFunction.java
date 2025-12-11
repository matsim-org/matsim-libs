package org.matsim.contrib.ev.strategic.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;

/**
 * This is a MATSim scoring function that integrates the score obtained from
 * charging scoring with a factor.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StrategicChargingScoringFunction implements BasicScoring {
	private final Id<Person> personId;
	private final double weight;

	private final ChargingPlanScoring scoring;

	public StrategicChargingScoringFunction(ChargingPlanScoring scoring, Id<Person> personId, double weight) {
		this.scoring = scoring;
		this.personId = personId;
		this.weight = weight;
	}

	@Override
	public void finish() {
		scoring.finalizeScoring();
	}

	@Override
	public double getScore() {
		return weight * scoring.getScore(personId);
	}

	static public class Factory implements ScoringFunctionFactory {
		private final ScoringFunctionFactory delegate;
		private final ChargingPlanScoring chargingScoring;

		private final double weight;

		public Factory(ScoringFunctionFactory delegate, ChargingPlanScoring chargingScoring, double weight) {
			this.delegate = delegate;
			this.chargingScoring = chargingScoring;
			this.weight = weight;
		}

		@Override
		public ScoringFunction createNewScoringFunction(Person person) {
			SumScoringFunction function = (SumScoringFunction) delegate.createNewScoringFunction(person);
			function.addScoringFunction(new StrategicChargingScoringFunction(chargingScoring, person.getId(), weight));
			return function;
		}
	}
}
