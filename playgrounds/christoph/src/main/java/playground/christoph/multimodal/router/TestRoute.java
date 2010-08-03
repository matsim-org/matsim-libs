package playground.christoph.multimodal.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.run.NetworkCleaner;

public class TestRoute {

	private static final Logger log = Logger.getLogger(TestRoute.class);
	
	private String networkFile = "../../matsim/mysimulations/multimodal/input_multi-modal/network.xml.gz";
	private String outFile = "../../matsim/mysimulations/multimodal/input_multi-modal/network_cleaned.xml.gz";
	
	public static void main(String[] args) {
		new TestRoute(new ScenarioImpl());
	}
	
	public TestRoute(Scenario scenario) {
		new NetworkCleaner().run(networkFile, outFile);

		log.info("Read Network File...");
		new MatsimNetworkReader(scenario).readFile(networkFile);
		log.info("done.");
		
		
		LeastCostPathCalculatorFactory factory = new DijkstraFactory();
		FreespeedTravelTimeCost travelTimeCost = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		
		PlansCalcRoute plansCalcRoute = new PlansCalcRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), travelTimeCost, travelTimeCost, factory);
		MultiModalPlansCalcRoute multiModalPlansCalcRoute = new MultiModalPlansCalcRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), travelTimeCost, travelTimeCost, factory);
		
//		2010-07-23 16:17:37,680  WARN Dijkstra:214 No route was found from node 17560001623057FTd to node 17560200524917
//		2010-07-23 16:17:50,078  WARN Dijkstra:214 No route was found from node 17560001589559FTd to node 17560200448173
//		2010-07-23 16:17:52,019  WARN Dijkstra:214 No route was found from node 17560200333610 to node 17560200342140
//		2010-07-23 16:17:52,125  WARN Dijkstra:214 No route was found from node 17560200507432 to node 17560200524789
//		2010-07-23 16:18:02,711  WARN Dijkstra:214 No route was found from node 17560200294509 to node 17560200036414
//		2010-07-23 16:18:14,379  WARN Dijkstra:214 No route was found from node 17560200504968 to node 17560200508718
		
		Node fromNode = scenario.getNetwork().getNodes().get(scenario.createId("17560001623057FTd"));
		Node toNode = scenario.getNetwork().getNodes().get(scenario.createId("17560200524917"));
		Path path = plansCalcRoute.getLeastCostPathCalculator().calcLeastCostPath(fromNode, toNode, 0.0);
		log.info("path: " + path);
		
		path = multiModalPlansCalcRoute.getLeastCostPathCalculator().calcLeastCostPath(fromNode, toNode, 0.0);
		log.info("path: " + path);
	}
}
