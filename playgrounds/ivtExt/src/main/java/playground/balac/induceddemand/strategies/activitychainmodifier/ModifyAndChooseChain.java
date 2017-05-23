package playground.balac.induceddemand.strategies.activitychainmodifier;

import java.util.HashMap;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;

public class ModifyAndChooseChain implements PlanAlgorithm {

	private NeighboursCreator nc;
	public ModifyAndChooseChain(Random localInstance, StageActivityTypes stageActivityTypes, Scenario scenario, 
			LeastCostPathCalculator pathCalculator, QuadTree shopFacilityQuadTree, QuadTree leisureFacilityQuadTree,
			ScoringFunctionFactory scoringFunctionFactory, HashMap scoreChange, ScoringParametersForPerson parametersForPerson
			, final TripRouter routingHandler, final ActivityFacilities facilities) {
		nc = new NeighboursCreator(stageActivityTypes, shopFacilityQuadTree, leisureFacilityQuadTree, 
				scenario, pathCalculator, scoringFunctionFactory, scoreChange, parametersForPerson, routingHandler, facilities);
	}	

	@Override
	public void run(Plan plan) {

		nc.findBestNeighbour(plan);
	}

}
