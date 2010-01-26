package org.matsim.core.scoring;

import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;


public class CharyparNagelScoringFunctionAccumulatorTest extends CharyparNagelScoringFunctionTest {
	@Override
	protected ScoringFunction getScoringFunctionInstance(final PlanImpl somePlan) {

		CharyparNagelScoringFunctionFactory charyparNagelScoringFunctionFactory = new CharyparNagelScoringFunctionFactory(this.config.charyparNagelScoring());

		return charyparNagelScoringFunctionFactory.getNewScoringFunction(somePlan);
	}
}
