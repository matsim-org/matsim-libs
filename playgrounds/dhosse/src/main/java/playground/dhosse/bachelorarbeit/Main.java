package playground.dhosse.bachelorarbeit;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class Main {

	public static void main(String args[]){
		
		String file1 = "./input/config.xml";
//		String file2 = "./input/network_bridge.xml";
		
//		Config config = ConfigUtils.createConfig();
		Config config = ConfigUtils.loadConfig(file1);
		Scenario sc = ScenarioUtils.loadScenario(config);
//		Scenario sc = ScenarioUtils.createScenario(config);
//		Scenario sc2 = ScenarioUtils.createScenario(config);
		
//		MatsimNetworkReader nr = new MatsimNetworkReader(sc);
//		nr.readFile(file1);

//		MatsimNetworkReader nr2 = new MatsimNetworkReader(sc2);
//		nr2.readFile(file2);
		
//		NetworkSimplifier simp = new NetworkSimplifier();
//		simp.run(sc.getNetwork());
		NetworkInspector nI = new NetworkInspector(sc);
//		nI.checkNetworkAttributes(true, true);
//		nI.isRoutable();
		nI.accessibilityComputation(true);

//		Grid grid = new Grid();
//		grid.calculateTravelTime(sc.getNetwork());
//		grid.gridComparison(1000, sc.getNetwork(), sc2.getNetwork());
//		grid.generateSHPExport(file1,file2);
		
	}
}