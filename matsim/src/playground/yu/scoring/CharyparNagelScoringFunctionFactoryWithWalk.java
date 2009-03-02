/**
 * 
 */
package playground.yu.scoring;

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;

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
