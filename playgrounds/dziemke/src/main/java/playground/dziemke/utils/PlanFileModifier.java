package playground.dziemke.utils;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 * 
 * Reads in a plans file and copies persons with their plans into a new plans file according to
 * configurable parameters. Then writes new plans file to a given location.
 */
public class PlanFileModifier {
	// Parameters
	static double selectionProbability = 1.;
	static boolean onlyTransferSelectedPlan = false;
	static boolean considerHomeStayingAgents = true;
	static boolean includeStayHomePlans = true;
	static boolean onlyConsiderPeopleAlwaysGoingByCar = true;
	static int maxNumberOfAgentsConsidered = 1000000;
	static String runId = "run_171";
	static int iteration = 300;
	
	
	// Input and output files
//	static String inputPlansFile = "D:/Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + iteration
//			+ "/" + runId + "." + iteration + ".plans.xml.gz";
	static String inputPlansFile = "D:/Workspace/data/cemdapMatsimCadyts/input/cemdap2matsim/28/plans.xml.gz";
//	static String outputPlansFile = "D:/Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + iteration
//			+ "/" + runId + "." + iteration + ".plans_selected.xml.gz";
	static String outputPlansFile = "D:/Workspace/data/cemdapMatsimCadyts/input/cemdap2matsim/28/plans_car.xml.gz";
	
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReaderMatsimV5 reader = new PopulationReaderMatsimV5(scenario);
		reader.readFile(inputPlansFile);
		Population population = scenario.getPopulation();
		
		
		Config config2 = ConfigUtils.createConfig();
		Scenario scenario2 = ScenarioUtils.createScenario(config2);
		Population population2 = scenario2.getPopulation();
		
		int counter = 0;
		
		for (Person person : population.getPersons().values()) {
			
			if (counter < maxNumberOfAgentsConsidered) {
				Random random = new Random();
				double randomNumber = random.nextDouble();
				
				boolean considerPerson = true;
				
				Plan selectedPlan = person.getSelectedPlan();
				int numberOfPlanElementsSelectedPlan = selectedPlan.getPlanElements().size();
				if (considerHomeStayingAgents == false) {
					if (numberOfPlanElementsSelectedPlan <= 1) {
						considerPerson = false;
					}
				}
				
				// ------------------------------------------------------------------------------------
				int numberOfPlans = person.getPlans().size();
				
				if (onlyConsiderPeopleAlwaysGoingByCar == true) {
					for (int i=0; i < numberOfPlans; i++) {
						//boolean considerPlan = true;
						
						Plan plan = person.getPlans().get(i);
						//int numberOfPlanElements = plan.getPlanElements().size();
						
						int numberOfPlanElements = plan.getPlanElements().size();
						for (int j=0; j < numberOfPlanElements; j++) {
							if (plan.getPlanElements().get(j) instanceof Leg) {
								LegImpl leg = (LegImpl) plan.getPlanElements().get(j);
								if (!leg.getMode().equals("car")) {
									considerPerson = false;
								}
							}
						}
						
					}
				} else {
					// do not switch "considerPerson"
				}
				
				
				// ------------------------------------------------------------------------------------
				
				if (randomNumber <= selectionProbability) {
					if (considerPerson == true) {
						Id<Person> id = person.getId();
						
						if (onlyTransferSelectedPlan == true) {
							Person person2 = population.getFactory().createPerson(id);
							person2.addPlan(selectedPlan);
							population2.addPerson(person2);
						} else {
							Person person2 = population.getFactory().createPerson(id);
							
							//int numberOfPlans = person.getPlans().size();
							for (int i=0; i < numberOfPlans; i++) {
								boolean considerPlan = true;
								
								Plan plan = person.getPlans().get(i);
								int numberOfPlanElements = plan.getPlanElements().size();
								
								if (includeStayHomePlans == false) {
									if (numberOfPlanElements <= 1) {
										considerPlan = false;
									}
								}
								
								if (considerPlan == true) {
									person2.addPlan(plan);
								}
							}
							population2.addPerson(person2);
						}
					}
				} else {
					// do nothing
				}
			}
			counter ++;
		}
						
		
		// write population file
		new PopulationWriter(scenario2.getPopulation(), null).write(outputPlansFile);
		
		System.out.println("Minimized plans file " + outputPlansFile + " written.");
	}
}