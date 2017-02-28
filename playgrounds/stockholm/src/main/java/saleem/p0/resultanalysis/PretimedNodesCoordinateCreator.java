package saleem.p0.resultanalysis;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import saleem.p0.stockholm.StockholmP0Helper;
import saleem.stockholmmodel.modelbuilding.PTCapacityAdjusmentPerSample;
/**
 * A class to calculate coordinates of signalised junctions using existing utility classes
 * 
 * @author Mohammad Saleem
 */
public class PretimedNodesCoordinateCreator {
	public static void main(String[] args){
		String path = "./ihop2/matsim-input/config - P0.xml";
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
		String nodesfile = "./ihop2/matsim-input/Nodes.csv";
		List<String> timednodes = sth.getPretimedNodes(nodesfile);
		String pretimedxyxcords = "./ihop2/matsim-input/pretimedxyxcords.xy";
		sth.writePretimedNodesCoordinates(nodesfile,pretimedxyxcords);

	}
}
