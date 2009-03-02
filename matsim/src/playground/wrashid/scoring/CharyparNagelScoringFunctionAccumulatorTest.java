package playground.wrashid.scoring;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.scoring.CharyparNagelScoringFunctionTest;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.scoring.ScoringFunction;

public class CharyparNagelScoringFunctionAccumulatorTest extends CharyparNagelScoringFunctionTest {
	protected ScoringFunction getScoringFunctionInstance(final Plan somePlan) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoringFunction( somePlan, new CharyparNagelScoringParameters(Gbl.getConfig().charyparNagelScoring())));
		
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoringFunction( somePlan, new CharyparNagelScoringParameters(Gbl.getConfig().charyparNagelScoring())));
		
		
		return scoringFunctionAccumulator;
	}
}
