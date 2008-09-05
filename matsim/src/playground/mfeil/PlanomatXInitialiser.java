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
import org.matsim.controler.*;

/**
 * @author Matthias Feil
 * Replacing the PlanomatOptimizeTimes class to include PlanomatX module.
 */
public class PlanomatXInitialiser extends MultithreadedModuleA{
	
	private LegTravelTimeEstimator estimator = null;
	private PreProcessLandmarks preProcessRoutingData;
	private NetworkLayer network;
	private TravelCost travelCostCalc;
	private TravelTime travelTimeCalc;

	public PlanomatXInitialiser (final ControlerTest controlerTest, final LegTravelTimeEstimator estimator) {
		this.estimator = estimator;
		preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		network = controlerTest.getNetwork();
		preProcessRoutingData.run(network);
		travelCostCalc = controlerTest.getTravelCostCalculator();
		travelTimeCalc = controlerTest.getTravelTimeCalculator();

	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {

		PlanAlgorithm planomatXAlgorithm = null;
		planomatXAlgorithm =  new PlanomatX(this.estimator, this.network, this.travelCostCalc, 
				this.travelTimeCalc, this.preProcessRoutingData);

		return planomatXAlgorithm;
	}

}
