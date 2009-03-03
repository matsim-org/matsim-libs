package playground.wrashid.scoring;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.CharyparNagelScoringFunctionTest;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.scoring.ScoringFunction;

public class CharyparNagelScoringFunctionAccumulatorTest extends CharyparNagelScoringFunctionTest {
	protected ScoringFunction getScoringFunctionInstance(final Plan somePlan) {

		CharyparNagelScoringFunctionFactory charyparNagelScoringFunctionFactory = new CharyparNagelScoringFunctionFactory(Gbl.getConfig().charyparNagelScoring());

		return charyparNagelScoringFunctionFactory.getNewScoringFunction(somePlan);
	}
}
