/**
 * 
 */
package playground.mfeil;

import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.*;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.scoring.*;

/**
 * @author Matthias Feil
 * Replacing the PlanomatOptimizeTimes class to initialise the PlanomatX module.
 */

public class PlanomatXInitialiser extends MultithreadedModuleA{
	
	private final LegTravelTimeEstimator 	estimator;
	private final PreProcessLandmarks 		preProcessRoutingData;
	private final NetworkLayer 				network;
	private final TravelCost 				travelCostCalc;
	private final TravelTime 				travelTimeCalc;
	private final ScoringFunctionFactory 	factory;

	
	public PlanomatXInitialiser (final ControlerTest controlerTest, final LegTravelTimeEstimator estimator) {
		
		this.estimator = estimator;
		preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		network = controlerTest.getNetwork();
		preProcessRoutingData.run(network);
		travelCostCalc = controlerTest.getTravelCostCalculator();
		travelTimeCalc = controlerTest.getTravelTimeCalculator();
		//factory = Gbl.getConfig().planomat().getScoringFunctionFactory();//TODO @MF: Check whether this is correct (Same scoring function as for Planomat)!
		factory = new CharyparNagelScoringFunctionFactory();
		}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {

		PlanAlgorithm planomatXAlgorithm = null;
		planomatXAlgorithm =  new PlanomatX2 (this.estimator, this.network, this.travelCostCalc, 
				this.travelTimeCalc, this.preProcessRoutingData, this.factory);

		return planomatXAlgorithm;
	}
}
