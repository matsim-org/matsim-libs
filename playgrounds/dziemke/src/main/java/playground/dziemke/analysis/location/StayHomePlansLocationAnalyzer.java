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
 * based on "playground.dziemke.pots.analysis.disaggregatedPotsdamDisaggregatedAnalysis"
 * analyzes home locations of those agents who stay home
 * based on plans of later iterations
 */
public class StayHomePlansLocationAnalyzer {
	// Parameters
	static String runId = "run_132";
	static int iteration = 150;
	
	// Input file and output directory
//	private static String inputPlansFile = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it." + iteration 
//			+ "/" + runId + "." + iteration + ".plans.xml.gz";
	private static String inputPlansFile = "D:/Workspace/container/demand/output/" + runId + "/" + runId + ".output_plans.xml.gz";
//	private static String outputDirectory = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it." + iteration + "/";
	private static String outputDirectory = "D:/Workspace/container/demand/output/" + runId + "/";
	
	
	public static void main(String[] args) {
		Map <Id<Person>, Coord> homeCoords = new HashMap <Id<Person>, Coord>();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReaderMatsimV5 reader = new PopulationReaderMatsimV5(scenario);
		reader.readFile(inputPlansFile);
		Population population = scenario.getPopulation();
		
		int selectedStayHomePlans = 0;
		
		for (Person person : population.getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();
			int numberOfPlanElements = selectedPlan.getPlanElements().size();
			
			if (numberOfPlanElements == 1) {
				selectedStayHomePlans++;
				//double score = selectedPlan.getScore();
				//System.out.println("Score of stay-home plan is " + score);
				
				Id<Person> id = person.getId();
				//TODO Now using 0th activity as home activity. Change it to what is specifically needed...
				Activity activity = (Activity) selectedPlan.getPlanElements().get(0);
				
				homeCoords.put(id, activity.getCoord());
			}
		}
		PointShapeFileWriter.writeShapeFilePoints(outputDirectory + "stayHome.shp", homeCoords, "AgentId");		
		System.out.println("Number of selected stay-home plans is " + selectedStayHomePlans);
	}
}