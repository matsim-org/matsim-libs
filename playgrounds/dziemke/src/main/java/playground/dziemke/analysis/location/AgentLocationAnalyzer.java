package playground.dziemke.analysis.location;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 * based on "playground.dziemke.pots.analysis.disaggregatedPotsdamDisaggregatedAnalysis"
 */
public class AgentLocationAnalyzer {
	// Parameters
	private static Id<Plan> id = Id.create("2000_200001", Plan.class);
	private static String activityLocationType = "home";
	
	// Input file and output directory
	private static String inputPlansFile = "D:/Workspace/container/demand/input/cemdap2matsim/17/plans.xml.gz";
	private static String outputFileBase = "D:/Workspace/container/demand/input/cemdap2matsim/17/locations/";
		
	
	public static void main(String[] args) {
		Map <Id<Plan>, Coord> coords = new HashMap <Id<Plan>, Coord>();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReaderMatsimV5 reader = new PopulationReaderMatsimV5(scenario);
		reader.readFile(inputPlansFile);
		Population population = scenario.getPopulation();

		Person person = population.getPersons().get(id);
		
		for (int planNumber = 0; planNumber < person.getPlans().size(); planNumber++) {
			Plan plan = person.getPlans().get(planNumber);
			Id<Plan> planId = Id.create(planNumber, Plan.class);
			
			boolean firstSuitableActivityLocationAlreadyFound = false;
			
			int numberOfPlanElements = plan.getPlanElements().size();
			
			for(int i=0; i < numberOfPlanElements; i++) {
				PlanElement planElement = plan.getPlanElements().get(i);
				if (firstSuitableActivityLocationAlreadyFound == false) {
					if (planElement instanceof Activity) {
						String activityType = ((Activity) planElement).getType();
						Activity activity = (Activity) planElement;
						if (activityType.equals(activityLocationType)) {
							firstSuitableActivityLocationAlreadyFound = true;						
							coords.put(planId, activity.getCoord());
						}
					}
				}
			}
		}
		PointShapeFileWriter.writeShapeFilePoints(outputFileBase + activityLocationType + "_" + id + ".shp", coords, "PlanId");		
	}
}