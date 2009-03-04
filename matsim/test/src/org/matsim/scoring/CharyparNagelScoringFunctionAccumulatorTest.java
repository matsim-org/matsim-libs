package org.matsim.scoring;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;


public class CharyparNagelScoringFunctionAccumulatorTest extends CharyparNagelScoringFunctionTest {
	protected ScoringFunction getScoringFunctionInstance(final Plan somePlan) {

		CharyparNagelScoringFunctionFactory charyparNagelScoringFunctionFactory = new CharyparNagelScoringFunctionFactory(Gbl.getConfig().charyparNagelScoring());

		return charyparNagelScoringFunctionFactory.getNewScoringFunction(somePlan);
	}
}
