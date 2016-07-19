package playground.dziemke.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class PlanDiversityAnalyzer {
	static String inputPlansFile = "D:/Workspace/container/demand/input/cemdap2matsim/22/plans.xml.gz";
	static boolean withStayHomePlans = true;
	
	// NOTE: as opposed to earlier, this class should also work for agents who DO HAVE a stay-home plan
	// however, only if there is only ONE stay-home plan (so far always the case)
	// and the stay-home plan is the last plan of an agent (so far it has always been like that, but there's no guarantee)
	
	// has been tested:
	// planFile with stay-home plan and option "withStayHomePlans=true" delivers same result as
	// similar planFile without stay-home plans and option "withStayHomePlans=false"
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReader reader = new PopulationReader(scenario);
		reader.readFile(inputPlansFile);
		Population population = scenario.getPopulation();
		
		int counterConstantNumberOfActivitiesOverPlans = 0;
		int counterVaryingNumberOfActivitiesOverPlans = 0;
		
		int counterConstantEndTimesOverPlans = 0;
		int counterVaryingEndTimesOverPlans = 0;
		
		// iterate over persons
		for (Person person : population.getPersons().values()) {
			int numberOfPlans = person.getPlans().size();
			
			if (withStayHomePlans == true) {
				numberOfPlans = numberOfPlans - 1;
			}
			
			int numberOfActivitiesFirstPlan = 0;
			boolean constantNumberOfActivitiesOverPlans = true;
			
			ObjectAttributes activityEndTimes = new ObjectAttributes();
			Map<Integer,Integer> numberOfActivitiesMap = new HashMap<Integer,Integer>();
			Map<Integer,Boolean> constantNumberOfActivitiesMap = new HashMap<Integer,Boolean>();
			
			// iterate over plans
			for (Integer planNumber=0; planNumber<numberOfPlans; planNumber++) {
				int numberOfPlanElements = person.getPlans().get(planNumber).getPlanElements().size();
				
				Integer numberOfActivities = 0;
				
				// iterate over plan elements
				for (Integer planElementNumber=0; planElementNumber<numberOfPlanElements; planElementNumber++) {
					PlanElement planElement = person.getPlans().get(planNumber).getPlanElements().get(planElementNumber);
					if (planElement instanceof Activity) {
						double endTime = ((Activity) planElement).getEndTime();
						activityEndTimes.putAttribute(planNumber.toString(), numberOfActivities.toString(), endTime);
						numberOfActivities++;
					}
				}
				
				if (planNumber == 0) {
					numberOfActivitiesFirstPlan = numberOfActivities;
				}
				
				if (numberOfActivitiesFirstPlan != numberOfActivities) {
					constantNumberOfActivitiesOverPlans = false;
				}				
				
				//System.out.println("numberOfActivities: " + numberOfActivities);
				numberOfActivitiesMap.put(planNumber, numberOfActivities);
				constantNumberOfActivitiesMap.put(planNumber, constantNumberOfActivitiesOverPlans);
			}
			
			if (constantNumberOfActivitiesOverPlans == true) {
				counterConstantNumberOfActivitiesOverPlans++;
			} else {
				counterVaryingNumberOfActivitiesOverPlans++;
			}
			
			
			boolean constantEndTimesOverPlans = true;
			// iterate over plans
			for (Integer planNumber=0; planNumber<numberOfPlans; planNumber++) {
				
				double endTimeInFirstPlan = 0.;
				double endTimeCurrentPlan = 0.;
				
				if (constantNumberOfActivitiesMap.get(planNumber) == true) {
					int numberOfActivities = numberOfActivitiesMap.get(planNumber);
									
					for (Integer activityNumber=0; activityNumber<numberOfActivities; activityNumber++) {
						endTimeInFirstPlan = (Double) activityEndTimes.getAttribute("0", activityNumber.toString());
						endTimeCurrentPlan = (Double) activityEndTimes.getAttribute(planNumber.toString(), activityNumber.toString());
						
					}
					if (endTimeInFirstPlan != endTimeCurrentPlan) {
						constantEndTimesOverPlans = false;
					}
				}
					
			}
			if (constantEndTimesOverPlans == true) {
				counterConstantEndTimesOverPlans++;
			} else {
				counterVaryingEndTimesOverPlans++;						
			}		
		}
		
		// amount of agents who have varying number of activities has to be subtracted since they have not been considered when analyzing
		// endTimes and. Therefore, they were counted by the default "true"
		int correctedCounterConstantEndTimesOverPlans = counterConstantEndTimesOverPlans - counterVaryingNumberOfActivitiesOverPlans;
				
		System.out.println("counterConstantNumberOfPlanElements: :" + counterConstantNumberOfActivitiesOverPlans);
		System.out.println("counterVaryingNumberOfPlanElements: " + counterVaryingNumberOfActivitiesOverPlans);
		
		System.out.println("correctedCounterConstantEndTimesOverPlans: :" + correctedCounterConstantEndTimesOverPlans);
		//System.out.println("counterConstantEndTimesOverPlans: :" + counterConstantEndTimesOverPlans);
		System.out.println("counterVaryingEndTimesOverPlans: " + counterVaryingEndTimesOverPlans);
	}
}