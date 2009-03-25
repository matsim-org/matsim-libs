package org.matsim.core.scoring;

import org.matsim.core.api.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;


public class CharyparNagelScoringFunctionAccumulatorTest extends CharyparNagelScoringFunctionTest {
	protected ScoringFunction getScoringFunctionInstance(final Plan somePlan) {

		CharyparNagelScoringFunctionFactory charyparNagelScoringFunctionFactory = new CharyparNagelScoringFunctionFactory(Gbl.getConfig().charyparNagelScoring());

		return charyparNagelScoringFunctionFactory.getNewScoringFunction(somePlan);
	}
}
