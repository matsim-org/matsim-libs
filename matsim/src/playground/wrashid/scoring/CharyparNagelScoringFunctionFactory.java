package playground.wrashid.scoring;

import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.gbl.Gbl;

public class CharyparNagelScoringFunctionFactory implements ScoringFunctionFactory {

	/**
	 * puts the scoring functions together, which form the
	 * CharyparScoringFunction
	 * 
	 * @param plan
	 * @return
	 */
	public ScoringFunction getNewScoringFunction(Plan plan) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoringFunction( plan, new CharyparNagelScoringParameters(Gbl.getConfig().charyparNagelScoring())));
		
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoringFunction( plan, new CharyparNagelScoringParameters(Gbl.getConfig().charyparNagelScoring())));
		
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoringFunction(new CharyparNagelScoringParameters(Gbl.getConfig().charyparNagelScoring())));
		
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoringFunction(new CharyparNagelScoringParameters(Gbl.getConfig().charyparNagelScoring())));
		
		return scoringFunctionAccumulator;
	}
}
