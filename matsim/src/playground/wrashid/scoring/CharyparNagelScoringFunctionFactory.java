package playground.wrashid.scoring;

import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.gbl.Gbl;

public class CharyparNagelScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
	
	public CharyparNagelScoringFunctionFactory(final CharyparNagelScoringConfigGroup config) {
		this.params = new CharyparNagelScoringParameters(config);
	}
	
	
	
	/**
	 * puts the scoring functions together, which form the
	 * CharyparScoringFunction
	 * 
	 * @param plan
	 * @return
	 */
	public ScoringFunction getNewScoringFunction(Plan plan) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoringFunction( plan, params));
		
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoringFunction( plan, params));
		
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoringFunction(params));
		
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoringFunction(params));
		
		return scoringFunctionAccumulator;
	}
}
