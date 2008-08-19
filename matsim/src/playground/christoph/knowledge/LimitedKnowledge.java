package playground.christoph.knowledge;

import org.matsim.network.NetworkLayer;
import org.matsim.replanning.modules.ReRouteDijkstra;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;

//============================================================================
// Knowledge Router
// Christoph Dobler, August 2008
//============================================================================

public class LimitedKnowledge extends ReRouteDijkstra {		

	// Erstmal alle Daten übernehmen... 
	public LimitedKnowledge(NetworkLayer network, TravelCostI travelCostCalc, TravelTimeI travelTimeCalc)
	{	
		super(network, travelCostCalc, travelTimeCalc);
		System.out.println("----------------------LimitedKnowledge Router runs!----------------------");
		
	}	


/*
public class LimitedKnowledge implements StrategyModuleI {

	@Override
	public void finish() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlePlan(Plan plan) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}
*/
}
