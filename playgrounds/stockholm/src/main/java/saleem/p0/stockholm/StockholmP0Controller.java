package saleem.p0.stockholm;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.InflowConstraint;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.VariableIntervalTimeVariantLinkFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;

import saleem.stockholmscenario.teleportation.PTCapacityAdjusmentPerSample;


public class StockholmP0Controller {

	public static void main(String[] args) {
		
//		String path = "./ihop2/matsim-input/config - P0.xml";
		String path = "./ihop2/matsim-input/configSingleJunction.xml";

//		String path = "H:\\Mike Work\\input\\config.xml";
		Config config = ConfigUtils.loadConfig(path);
		config.network().setTimeVariantNetwork(true);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
	    double samplesize = config.qsim().getStorageCapFactor();
		
		// Changing vehicle and road capacity according to sample size
		PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
		capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
		
		Network network = (Network)scenario.getNetwork();
		StockholmP0Helper sth = new StockholmP0Helper(network);
//		String nodesfile = "./ihop2/matsim-input/Nodes.csv";
		String nodesfile = "./ihop2/matsim-input/NodesSingleJunction.csv";

		List<String> timednodes = sth.getPretimedNodes(nodesfile);
//		List<String> timednodes = sth.getPretimedNodes("H:\\Mike Work\\input\\Nodes2Junctions.csv");
		
		Map<String, List<Link>> incominglinks = sth.getInLinksForJunctions(timednodes, network);
		Map<String, List<Link>> outgoinglinks = sth.getOutLinksForJunctions(timednodes, network);

		//		String pretimedxyxcords = "./ihop2/matsim-input/pretimedxyxcords.xy";
//		sth.writePretimedNodesCoordinates(nodesfile,pretimedxyxcords);
		
		NetworkFactory nf = network.getFactory();
		nf.setLinkFactory(new VariableIntervalTimeVariantLinkFactory());
		
		controler.getConfig().qsim().setInflowConstraint(InflowConstraint.maxflowFromFdiag);
		controler.getConfig().qsim().setTrafficDynamics(TrafficDynamics.withHoles);
//		
		
		controler.addControlerListener(new StockholmP0ControlListener(scenario, (Network) scenario.getNetwork(), incominglinks, outgoinglinks));
//		controler.setModules(new ControlerDefaultsWithRoadPricingModule());
		controler.run();
		
	}
}
