package saleem.p0.stockholm;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.VariableIntervalTimeVariantLinkFactory;
import org.matsim.core.scenario.ScenarioUtils;

import saleem.p0.GenericP0ControlListener;
import saleem.stockholmscenario.teleportation.PTCapacityAdjusmentPerSample;

public class StockholmP0Controller {

	public static void main(String[] args) {
		String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\config - P0.xml";
//		String path = "H:\\Mike Work\\input\\config.xml";
		Config config = ConfigUtils.loadConfig(path);
		config.network().setTimeVariantNetwork(true);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
	    double samplesize = config.qsim().getStorageCapFactor();
		
		// Changing vehicle and road capacity according to sample size
		PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
		capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
		
		NetworkImpl network = (NetworkImpl)scenario.getNetwork();
		StockholmP0Helper sth = new StockholmP0Helper(network);
		List<String> timednodes = sth.getPretimedNodes("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\Nodes.csv");
//		List<String> timednodes = sth.getPretimedNodes("H:\\Mike Work\\input\\Nodes2Junctions.csv");
		Map<String, List<Link>> incominglinks = sth.getInLinksForJunctions(timednodes, network);
		Map<String, List<Link>> outgoinglinks = sth.getOutLinksForJunctions(timednodes, network);
		NetworkFactoryImpl nf = network.getFactory();
		nf.setLinkFactory(new VariableIntervalTimeVariantLinkFactory());
		controler.addControlerListener(new StockholmP0ControlListener(scenario, (NetworkImpl) scenario.getNetwork(), incominglinks, outgoinglinks));
		controler.run();
		
	}
}
