package playground.sergioo.workplaceCapacities.gui;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class Paint3DBuildings {

	//Constants
	private static final String NETWORK_FILE = "./data/currentSimulation/singapore2.xml";
	private static final String WORK_FACILITIES_FILEO = "./data/facilities/workFacilitiesO.xml";

	//Attributes

	//Main
	public static void main(String[] args) {
		String[] schedules = {"w_0945_0800", "w_0900_1015", "w_0830_0915", "w_0730_1000"};
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(NETWORK_FILE);
		new MatsimFacilitiesReader(scenario).readFile(WORK_FACILITIES_FILEO);
		WorkersBSPainter painter = new WorkersBSPainter(scenario.getNetwork());
		painter.setData(scenario.getActivityFacilities(), schedules);
		new BSSimpleNetworkWindow("Building capacities", painter).setVisible(true);
	}

}
