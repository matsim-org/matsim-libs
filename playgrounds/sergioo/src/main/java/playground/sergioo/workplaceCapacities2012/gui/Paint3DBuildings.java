package playground.sergioo.workplaceCapacities2012.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.MatsimFacilitiesReader;

public class Paint3DBuildings {

	//Constants
	private static final String NETWORK_FILE = "./data/currentSimulation/singapore2.xml";
	private static final String WORK_FACILITIES_FILEO = "./data/facilities/workFacilitiesO.xml";
	private static final String WORK_FACILITIES_FILE = "./data/facilities/workFacilities.xml";

	//Attributes

	//Main
	public static void main(String[] args) throws FileNotFoundException {
		//String[] schedules = {"w_0945_0800", "w_0900_1015", "w_0830_0915", "w_0730_1000"};
		String[] schedules = {"w_0645_0815", "w_0730_1000", "w_0730_1145", "w_0730_1345", "w_0830_0915", "w_0900_1015", "w_0945_0800", "w_0945_1145", "w_1345_0845", "w_2015_0945"};
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(NETWORK_FILE);
		new MatsimFacilitiesReader(scenario).readFile(WORK_FACILITIES_FILEO);
		/*PrintWriter printWriter = new PrintWriter(new File("./data/workCapacitiesVis2.csv"));
		printWriter.print("id,x,y");
		for(String schedule:schedules)
			printWriter.print(","+schedule);
		printWriter.println(",total");
		for(ActivityFacility facility:scenario.getActivityFacilities().getFacilities().values()) {
			printWriter.print(facility.getId().toString()+","+facility.getCoord().getX()+","+facility.getCoord().getY());
			double total = 0;
			for(String schedule:schedules) {
				ActivityOption option = facility.getActivityOptions().get(schedule);
				if(option==null)
					printWriter.print(","+0);
				else {
					printWriter.print(","+option.getCapacity());
					total += option.getCapacity();
				}
			}
			printWriter.println(","+total);
		}
		printWriter.close();*/
		WorkersBSPainter painter = new WorkersBSPainter(scenario.getNetwork());
		painter.setData(scenario.getActivityFacilities(), schedules);
		new BSSimpleNetworkWindow("Building capacities", painter).setVisible(true);
	}

}
