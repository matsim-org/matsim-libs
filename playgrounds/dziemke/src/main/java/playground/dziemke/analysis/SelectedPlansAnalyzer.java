package playground.dziemke.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dziemke
 * NOTE: Only yields useful results if agents cannot add activities to their plans over the course of the simulation
 */
public class SelectedPlansAnalyzer {

	public static final Logger log = Logger.getLogger(SelectedPlansAnalyzer.class);

	// Parameters
	private static String runId = "be_117ja"; // <----------
	private static int numberOfIterations = 300; // <----------
	private static int plansFileInterval = 300; // <----------
	private static boolean useInterimPlans = false;
	private static boolean useOutputPlans = true;
	
	// Input/output
//	private static final  String directoryRoot = "../../../runs-svn/cemdapMatsimCadyts/" + runId;
	private static String directoryRoot = "../../../runs-svn/berlin_scenario_2016/" + runId;

	private static String alternativeOutputDir = null;

	private static final  Map<Integer, Integer> stayHomePlansMap = new HashMap<>();
	private static final  Map<Integer, Integer> otherPlansMap = new HashMap<>();
	private static final  Map<Integer, Integer> carPlansMap = new HashMap<>();
	private static final  Map<Integer, Integer> ptPlansMap = new HashMap<>();
	private static final  Map<Integer, Integer> slowPtPlansMap = new HashMap<>();
	private static final  Map<Integer, Integer> walkPlansMap = new HashMap<>();
	
	
	public static void main(String[] args) {
		if (args.length != 0) {
			directoryRoot = args[0];
			runId = args[1];
			numberOfIterations = Integer.valueOf(args[2]);
			plansFileInterval = Integer.valueOf(args[3]);
			useInterimPlans = Boolean.valueOf(args[4]);
			useOutputPlans = Boolean.valueOf(args[5]);
			if (args.length >= 7) {
                alternativeOutputDir = args[6];
                log.info("AlternativeOutputDir: " + alternativeOutputDir);
            }
        }
        run();
	}

	public static void run() {
		if (useInterimPlans) {
			for (int i = 1; i<= numberOfIterations/plansFileInterval; i++) {
				//String plansFile = "D:/Workspace/data/cemdapMatsimCadyts/output/" + runId + "/ITERS/it." + i * plansFileInterval
				String plansFile = directoryRoot + "/ITERS/it." + i * plansFileInterval
						+ "/" + runId + "." + i * plansFileInterval + ".plans.xml.gz";

				Config config = ConfigUtils.createConfig();
				Scenario scenario = ScenarioUtils.createScenario(config);
				PopulationReader reader = new PopulationReader(scenario);
				reader.readFile(plansFile);
				Population population = scenario.getPopulation();

				countSelectedPlanTypes(population, i * plansFileInterval);
			}
		}

		if (useOutputPlans) {
			String plansFileOutput = directoryRoot + "/" + runId + ".output_plans.xml.gz";
			//String plansFileOutput = "D:/Workspace/data/cemdapMatsimCadyts/output/" + runId + "/" + runId + ".output_plans.xml.gz";

			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
			PopulationReader reader = new PopulationReader(scenario);
			reader.readFile(plansFileOutput);
			Population population = scenario.getPopulation();

			countSelectedPlanTypes (population, 99999);
		}

		writeFile();
	}
	
	private static void countSelectedPlanTypes (Population population, int iteration) {
		int counterStayHomePlans = 0;
		int counterOtherPlans = 0;
		int counterCarPlans = 0;
		int counterPtPlans = 0;
		int counterPtSlowPlans = 0;
		int counterWalkPlans = 0;
		String mode;
		
		// iterate over persons
		for (Person person : population.getPersons().values()) {			
			Plan selectedPlan = person.getSelectedPlan();
			int numberOfPlanElements = selectedPlan.getPlanElements().size();
			
			if (numberOfPlanElements == 1) {
				counterStayHomePlans++;
			} else if (numberOfPlanElements > 1) {
				counterOtherPlans++;
				
				// ------------------------------------------------------------------------------------
				
				for (int i = 0; i< numberOfPlanElements; i++) {
					if (selectedPlan.getPlanElements().get(i) instanceof Leg) {
						Leg leg = (Leg) selectedPlan.getPlanElements().get(i);
						
						mode = leg.getMode();

						switch (mode) {
							case "car":
								counterCarPlans++;
								break;
							case "pt":
								counterPtPlans++;
								break;
							case "ptSlow":
								counterPtSlowPlans++;
								break;
							case "walk":
								counterWalkPlans++;
								break;
							default:
								throw new RuntimeException("Unknown mode: " + mode + ". In current implementation leg mode must either be car, pt, slowPt or walk");
						}
						
						// Break bricht die aktuelle Schleife ab; Continue leitet einen neuen Durchlauf ein.
						// break wird hier benutzt, weil "ChangeLegModes" den Mode für ALLE Plaene eines Agenten an einem
						// gegebenen Tag verändert. Insofern genuegt es das erste Leg zu betrachten.
						break;
					}
				}				
				
				// ------------------------------------------------------------------------------------
				
			} else {
				System.err.println("Plan may not have less than one element.");
			}		
		}
		stayHomePlansMap.put(iteration, counterStayHomePlans);
		otherPlansMap.put(iteration, counterOtherPlans);
		carPlansMap.put(iteration, counterCarPlans);
		ptPlansMap.put(iteration, counterPtPlans);
		slowPtPlansMap.put(iteration, counterPtSlowPlans);
		walkPlansMap.put(iteration, counterWalkPlans);
	}

	
	private static void writeFile() {

		String path = directoryRoot + "/analysis";
		if (alternativeOutputDir != null)
			path = alternativeOutputDir + "/analysis";

		if (new File(path).mkdirs()) {
			log.info(path + " was created");
		} else {
			log.warn(path + " was not created");
		}

		BufferedWriter bufferedWriter = null;
			
		try {
			File output = new File(path + "/selectedPlans.txt");
			FileWriter fileWriter = new FileWriter(output);
			bufferedWriter = new BufferedWriter(fileWriter);
			
			// Header
			bufferedWriter.write("It." + "\t" + "stayHomePlans" + "\t" + "otherPlans" + "\t" 
					+ "carPlans" + "\t" + "ptPlans" + "\t" + "slowPtPlans" + "\t" + "walkPlans");
			bufferedWriter.newLine();
			
			for (int iteration : stayHomePlansMap.keySet()) {
    			bufferedWriter.write(iteration + "\t" + stayHomePlansMap.get(iteration) + "\t" + otherPlansMap.get(iteration) + "\t" 
    					+ carPlansMap.get(iteration) + "\t" + ptPlansMap.get(iteration) + "\t" + slowPtPlansMap.get(iteration) + "\t"
    					+ walkPlansMap.get(iteration));
    			bufferedWriter.newLine();
    		}    		
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    } finally {
	        try {
	            if (bufferedWriter != null) {
	                bufferedWriter.flush();
	                bufferedWriter.close();
	            }
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	    }
		System.out.println("Analysis file " + path + "/selectedPlans.txt" + " written.");
	}
}