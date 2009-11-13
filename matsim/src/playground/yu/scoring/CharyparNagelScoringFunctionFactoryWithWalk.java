/**
 * 
 */
package playground.yu.scoring;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;

/**
 * change scoring function, because "walk"-mode will be implemented
 * 
 * @author yu
 */
public class CharyparNagelScoringFunctionFactoryWithWalk implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
	
	public CharyparNagelScoringFunctionFactoryWithWalk(final CharyparNagelScoringConfigGroup config) {
		this.params = new CharyparNagelScoringParameters(config);
	}
	
	public ScoringFunction getNewScoringFunction(Plan plan) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(plan, params));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelScoringFunctionWithWalk(plan, params));
		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(params));
		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(params));
		return scoringFunctionAccumulator;
	}

}
