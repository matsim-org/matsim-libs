package playground.dziemke.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Random;

/**
 * @author dziemke
 * 
 * Reads in a plans file and copies persons with their plans into a new plans file according to
 * configurable parameters. Then writes new plans file to a given location.
 */
public class PlanFileModifier {
	private final static Logger log = Logger.getLogger(PlanFileModifier.class);
	
	// Parameters
	static double selectionProbability = 1.;
	static boolean onlyTransferSelectedPlan = false;
	static boolean considerHomeStayingAgents = true;
	static boolean includeStayHomePlans = true;
	static boolean onlyConsiderPeopleAlwaysGoingByCar = false;
	static int maxNumberOfAgentsConsidered = 10000000;
//	static String runId = "run_194";
//	static int iteration = 300;
	static boolean removeLinksAndRoutes = true;
	
	
	// Input and output files
//	static final String INPUT_PLANS_FILE = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + iteration
//			+ "/" + runId + "." + iteration + ".plans.xml.gz";
//	static final String OUTPUT_PLANS_FILE = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + iteration
//			+ "/" + runId + "." + iteration + ".plans_selected_no_links_routes.xml.gz";
//	static final String INPUT_PLANS_FILE = "../../../shared-svn/projects/tum-with-moeckel/data/"
//			+ "mstm_run/run_04/siloMatsim/population_2000.xml";
//	static final String OUTPUT_ROOT = "../../../../SVN/shared-svn/projects/tum-with-moeckel/data/"
//			+ "mstm_run/run_04/siloMatsim/population_2000_half/";
//	static final String OUTPUT_PLANS_FILE = OUTPUT_ROOT + "population.xml";
	
	// In case using input plans that have not yet been iterated
//	static final String INPUT_PLANS_FILE = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap2matsim/30/plans.xml.gz";
//	static final String OUTPUT_PLANS_FILE = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap2matsim/30/plans_no_links_routes.xml.gz";
//	static String inputPlansFile = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/matsim_initial/100/plans.xml.gz";
//	static String outputPlansFile = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/matsim_initial/100/plans_no_links_routes.xml.gz";
	static String inputPlansFile = "../../../runs-svn/berlin_scenario_2016/be_118/be_118.output_plans.xml.gz";
	static String outputPlansFile = "../../../runs-svn/berlin_scenario_2016/be_118/be_118.output_plans_no_links_routes.xml.gz";
	
	
//	if (onlyTransferSelectedPlan == true) {
//		outputPlansFile = outputPlansFile + "_selected";
//	}
//	
//	outputPlansFile = outputPlansFile + ".xml.gz";
//	static String outputPlansFile = "D:/Workspace/data/cemdapMatsimCadyts/input/cemdap2matsim/28/plans_car.xml.gz";
	
	
	public static void main(String[] args) {
		if (args.length != 0) {
			inputPlansFile = args[0];
			outputPlansFile = args[1];
			selectionProbability = Double.parseDouble(args[2]);
			onlyTransferSelectedPlan = Boolean.parseBoolean(args[3]);
			considerHomeStayingAgents = Boolean.parseBoolean(args[4]);
			includeStayHomePlans = Boolean.parseBoolean(args[5]);
			onlyConsiderPeopleAlwaysGoingByCar = Boolean.parseBoolean(args[6]);
			maxNumberOfAgentsConsidered = Integer.parseInt(args[7]);
			removeLinksAndRoutes = Boolean.parseBoolean(args[8]);
		}

//		LogToOutputSaver.setOutputDirectory(OUTPUT_ROOT);
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReader reader = new PopulationReader(scenario);
		reader.readFile(inputPlansFile);
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
				if (!considerHomeStayingAgents) {
					if (numberOfPlanElementsSelectedPlan <= 1) {
						considerPerson = false;
					}
				}
				
				int numberOfPlans = person.getPlans().size();
				
				if (onlyConsiderPeopleAlwaysGoingByCar) {
					for (int i=0; i < numberOfPlans; i++) {
						//boolean considerPlan = true;
						
						Plan plan = person.getPlans().get(i);
						//int numberOfPlanElements = plan.getPlanElements().size();
						
						int numberOfPlanElements = plan.getPlanElements().size();
						for (int j=0; j < numberOfPlanElements; j++) {
							if (plan.getPlanElements().get(j) instanceof Leg) {
								Leg leg = (Leg) plan.getPlanElements().get(j);
								if (!leg.getMode().equals(TransportMode.car)) {
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
					if (considerPerson) {
						Id<Person> id = person.getId();
						Person person2 = population.getFactory().createPerson(id);
						
						if (onlyTransferSelectedPlan) {
//							Person person2 = population.getFactory().createPerson(id);
							if (removeLinksAndRoutes) {
								removeLinksAndRoutes(selectedPlan);
							}
							person2.addPlan(selectedPlan);
							population2.addPerson(person2);
						} else {
//							Person person2 = population.getFactory().createPerson(id);
							
							//int numberOfPlans = person.getPlans().size();
							for (int i=0; i < numberOfPlans; i++) {
								boolean considerPlan = true;
								
								Plan plan = person.getPlans().get(i);
								int numberOfPlanElements = plan.getPlanElements().size();
								
								if (!includeStayHomePlans) {
									if (numberOfPlanElements <= 1) {
										considerPlan = false;
									}
								}
								
								if (considerPlan) {
									if (removeLinksAndRoutes) {
										removeLinksAndRoutes(plan);
									}
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
		new PopulationWriter(scenario2.getPopulation(), null).write(outputPlansFile);
		
		log.info("Modified plans file contains " + agentCounter + " agents.");
		log.info("Modified plans file has been written to " + outputPlansFile);
	}
	
	
	private static void removeLinksAndRoutes(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				// remove link
				((Activity) pe).setLinkId(null);
			}
			if (pe instanceof Leg) {
				// remove route
				((Leg) pe).setRoute(null);
			}
		}
	}
}