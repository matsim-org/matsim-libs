package playground.wrashid.scoring;

import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.gbl.Gbl;

public class CharyparScoringFunctionFactory implements ScoringFunctionFactory {

	/**
	 * puts the scoring fucntions together, which form the
	 * CharyparScoringFunction
	 * 
	 * @param plan
	 * @return
	 */
	public ScoringFunction getNewScoringFunction(Plan plan) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoringFunction(plan));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoringFunction( plan, new CharyparNagelScoringParameters(Gbl.getConfig().charyparNagelScoring())));

		return scoringFunctionAccumulator;
	}
}
