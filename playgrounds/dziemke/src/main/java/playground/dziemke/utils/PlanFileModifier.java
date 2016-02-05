package playground.dziemke.utils;

import java.util.Random;

import org.apache.log4j.Logger;
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
	private final static Logger log = Logger.getLogger(PlanFileModifier.class);
	
	// Parameters
	static double selectionProbability = 0.5;
	static boolean onlyTransferSelectedPlan = true;
	static boolean considerHomeStayingAgents = true;
	static boolean includeStayHomePlans = true;
	static boolean onlyConsiderPeopleAlwaysGoingByCar = false;
	static int maxNumberOfAgentsConsidered = 1000000;
//	static String runId = "run_145";
//	static int iteration = 150;
	
	
	// Input and output files
//	static final String INPUT_PLANS_FILE = "D:/Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + iteration
//			+ "/" + runId + "." + iteration + ".plans.xml.gz";
//	static final String OUTPUT_PLANS_FILE = "D:/Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + iteration
//			+ "/" + runId + "." + iteration + ".plans_selected2.xml.gz";
	static final String INPUT_PLANS_FILE = "../../../../SVN/shared-svn/projects/tum-with-moeckel/data/"
			+ "mstm_run/run_04/siloMatsim/population_2000.xml";
	static final String OUTPUT_ROOT = "../../../../SVN/shared-svn/projects/tum-with-moeckel/data/"
			+ "mstm_run/run_04/siloMatsim/population_2000_half/";
	static final String OUTPUT_PLANS_FILE = OUTPUT_ROOT + "population.xml";
	
	
//	if (onlyTransferSelectedPlan == true) {
//		outputPlansFile = outputPlansFile + "_selected";
//	}
//	
//	outputPlansFile = outputPlansFile + ".xml.gz";
//	static String outputPlansFile = "D:/Workspace/data/cemdapMatsimCadyts/input/cemdap2matsim/28/plans_car.xml.gz";
	
	
	public static void main(String[] args) {
		LogToOutputSaver.setOutputDirectory(OUTPUT_ROOT);
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReaderMatsimV5 reader = new PopulationReaderMatsimV5(scenario);
		reader.readFile(INPUT_PLANS_FILE);
		Population population = scenario.getPopulation();
		
		
		Config config2 = ConfigUtils.createConfig();
		Scenario scenario2 = ScenarioUtils.createScenario(config2);
		Population population2 = scenario2.getPopulation();
		
		int agentCounter = 0;
		
		for (Person person : population.getPersons().values()) {
			
			if (agentCounter < maxNumberOfAgentsConsidered) {
				
				// Handle filters
				Random random = new Random();
				boolean considerPerson = true;
				
				Plan selectedPlan = person.getSelectedPlan();
				int numberOfPlanElementsSelectedPlan = selectedPlan.getPlanElements().size();
				if (considerHomeStayingAgents == false) {
					if (numberOfPlanElementsSelectedPlan <= 1) {
						considerPerson = false;
					}
				}
				
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
				}
				
				
				/*
				 * Create a copy of the person -- if selected according to all criteria -- and add it
				 * to new population
				 */
				if (random.nextDouble() <= selectionProbability) {
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
				} 
			}
			agentCounter ++;
		}
						
		
		// write population file
		new PopulationWriter(scenario2.getPopulation(), null).write(OUTPUT_PLANS_FILE);
		
		log.info("Modified plans file contains " + agentCounter + " agents.");
		log.info("Modified plans file has been written to " + OUTPUT_PLANS_FILE);
	}
}