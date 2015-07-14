package playground.christoph.evacuation.pt;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;

import playground.christoph.evacuation.pt.TransitRouterNetworkThinner.RemoveRedundantDistanceLinks;
import playground.christoph.evacuation.pt.TransitRouterNetworkThinner.RemoveRedundantLinks;

public class TransitDebugger {

	private static final Logger log = Logger.getLogger(TransitDebugger.class);
		
	public static void main(String[] args) {	
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile("../../matsim/mysimulations/census2000V2/input_10pct/OeV/schedule_dummy.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		TransitRouterConfig c = new TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
	
		log.info("Writing debug transit router network to file...");
		TransitRouterNetwork transitRouterNetwork = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), c.getBeelineWalkConnectionDistance());
		new TransitRouterNetworkWriter(transitRouterNetwork).write("../../matsim/mysimulations/census2000V2/input_10pct/OeV/debug_network.xml");
		log.info("done.");
		
		log.info("Removing redundant links...");
		new RemoveRedundantLinks().run(transitRouterNetwork);
		new TransitRouterNetworkWriter(transitRouterNetwork).write("../../matsim/mysimulations/census2000V2/input_10pct/OeV/debug_network_removed_redundant.xml");		
		log.info("done.");
		
		log.info("Removing redundant distance links...");
		new RemoveRedundantDistanceLinks().run(transitRouterNetwork);
		new TransitRouterNetworkWriter(transitRouterNetwork).write("../../matsim/mysimulations/census2000V2/input_10pct/OeV/debug_network_removed_redundant_distance.xml");		
		log.info("done.");
	}
}
