/**
 * 
 */
package playground.yu.scoring;

import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.ScoringFunction;

/**
 * change scoring function, because "walk"-mode will be implemented
 * 
 * @author yu
 * 
 */
public class CharyparNagelScoringFunctionFactoryWithWalk extends
		CharyparNagelScoringFunctionFactory {

	@Override
	public ScoringFunction getNewScoringFunction(Plan plan) {
		return new CharyparNagelScoringFunctionWithWalk(plan);
	}

}
