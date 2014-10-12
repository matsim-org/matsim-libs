package playground.dziemke.analysis.location;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 */
public class HomeLocationAnalyzer {
	// Input file and output directory
	private static String inputPlansFile = "D:/Workspace/container/demand/input/hwh/population3.xml";
	private static String outputDirectory = "D:/Workspace/container/demand/output/run_142/analysis/";
	
	
	public static void main(String[] args) {
		Map <Id<Person>, Coord> homeCoords = new HashMap <Id<Person>, Coord>();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReaderMatsimV5 reader = new PopulationReaderMatsimV5(scenario);
		reader.readFile(inputPlansFile);
		Population population = scenario.getPopulation();
		
		int consideredAgents = 0;
		
		for (Person person : population.getPersons().values()) {
			consideredAgents++;
			Plan selectedPlan = person.getSelectedPlan();
			Id<Person> id = person.getId();
			//TODO Now using 0th activity as home activity. Change it to what is specifically needed...
			Activity activity = (Activity) selectedPlan.getPlanElements().get(0);
			homeCoords.put(id, activity.getCoord());
		}
		PointShapeFileWriter.writeShapeFilePoints(outputDirectory + "HomeLocations.shp", homeCoords, "AgentId");		
		System.out.println("Number of considered agents is " + consideredAgents);
	}
}