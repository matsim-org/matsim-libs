/**
 * 
 */
package playground.yu.scoring;

import org.matsim.core.api.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

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
		return new CharyparNagelScoringFunctionWithWalk(plan, this.params);
	}

}
