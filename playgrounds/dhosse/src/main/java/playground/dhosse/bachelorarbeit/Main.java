package playground.dhosse.bachelorarbeit;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class Main {
	
	public static void main(String args[]) {
		
//		String file1 = "C:/Users/Daniel/Dropbox/bsc/input/config.xml";
		String path = "C:/Users/Daniel/Dropbox/bsc/input";
		String file1 = "berlin_fis";
		String file2 = "berlin_osm";
		String file3 = "berlin_osm_main";
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		MatsimPopulationReader pr = new MatsimPopulationReader(scenario);
		MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
		nr.readFile(path+"/"+file3+".xml");
//		pr.readFile(path+"/"+"/test_population.xml");
		
//		NetworkBoundaryBox bbox = new NetworkBoundaryBox();
//		bbox.setDefaultBoundaryBox(scenario.getNetwork());
//		
//		ZoneLayer<Id> measuringPoints = GridUtils.createGridLayerByGridSizeByNetwork(50, bbox.getBoundingBox());
//		SpatialGrid freeSpeedGrid = new SpatialGrid(bbox.getBoundingBox(), 50);
//		InternalConstants.setOpusHomeDirectory("C:/Users/Daniel/Dropbox/bsc");
//		AccessibilityCalc ac = new AccessibilityCalc(measuringPoints, freeSpeedGrid, (ScenarioImpl) scenario, file1);
//		ac.runAccessibilityComputation();
		
		NetworkInspector ni = new NetworkInspector(scenario);
		if(ni.isRoutable())
			System.out.println("Netzwerk ist routbar...");
		else
			System.out.println("Netzwerk ist nicht routbar");
//		ni.checkNodeAttributes();
//		ni.checkLinkAttributes();
//		ni.shpExportNodeStatistics(ni.getRedundantNodes());
		
	}

}