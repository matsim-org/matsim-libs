/**
 * 
 */
package playground.mfeil;

import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.*;

/**
 * @author Matthias Feil
 * Replacing the PlanomatOptimizeTimes class to include PlanomatX module.
 */
public class PlanomatXInitialiser extends MultithreadedModuleA{
	
	private LegTravelTimeEstimator estimator = null;

	public PlanomatXInitialiser (final LegTravelTimeEstimator estimator) {
		this.estimator = estimator;
//		PlanomatConfig.init();
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {

		PlanAlgorithm planomatAlgorithm = null;
		planomatAlgorithm =  new PlanomatX(this.estimator);

		return planomatAlgorithm;
	}

}
