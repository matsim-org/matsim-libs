package playground.balac.induceddemand.strategies.activitychainmodifier;

import java.util.HashMap;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ModifyAndChooseChain implements PlanAlgorithm {

	private NeighboursCreator nc;
	public ModifyAndChooseChain(Random localInstance, StageActivityTypes stageActivityTypes, Scenario scenario, 
			LeastCostPathCalculator pathCalculator, QuadTree shopFacilityQuadTree, QuadTree leisureFacilityQuadTree,
			ScoringFunctionFactory scoringFunctionFactory, HashMap scoreChange, CharyparNagelScoringParametersForPerson parametersForPerson) {
		nc = new NeighboursCreator(stageActivityTypes, shopFacilityQuadTree, leisureFacilityQuadTree, 
				scenario, pathCalculator, scoringFunctionFactory, scoreChange, parametersForPerson);
	}	

	@Override
	public void run(Plan plan) {

		nc.findBestNeighbour(plan);
	}

}
