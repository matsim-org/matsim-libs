package org.matsim.scoring;

import org.matsim.core.api.population.Plan;
import org.matsim.gbl.Gbl;
import org.matsim.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;


public class CharyparNagelScoringFunctionAccumulatorTest extends CharyparNagelScoringFunctionTest {
	protected ScoringFunction getScoringFunctionInstance(final Plan somePlan) {

		CharyparNagelScoringFunctionFactory charyparNagelScoringFunctionFactory = new CharyparNagelScoringFunctionFactory(Gbl.getConfig().charyparNagelScoring());

		return charyparNagelScoringFunctionFactory.getNewScoringFunction(somePlan);
	}
}
