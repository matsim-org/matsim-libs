package playground.dziemke.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 * NOTE: Only yields useful results if agents cannot add activities to their plans over the course of the simulation
 */
public class SelectedPlansAnalyzer {
	// Parameters
	private static final  String runId = "run_162";
	private static final  int numberOfIterations = 100;
	private static final  int plansFileInterval = 100;
	private static final  boolean useInterimPlans = true;
	private static final  boolean useOutputPlans = false;
	
	// Input/output
	private static final  String directoryRoot = "../../../runs-svn/cemdapMatsimCadyts/" + runId;

	private static final  Map<Integer, Integer> stayHomePlansMap = new HashMap<Integer, Integer>();
	private static final  Map<Integer, Integer> otherPlansMap = new HashMap<Integer, Integer>();
	private static final  Map<Integer, Integer> carPlansMap = new HashMap<Integer, Integer>();
	private static final  Map<Integer, Integer> ptPlansMap = new HashMap<Integer, Integer>();
	private static final  Map<Integer, Integer> walkPlansMap = new HashMap<Integer, Integer>();
	
	
	public static void main(String[] args) {
		if (useInterimPlans == true) {
			for (int i = 1; i<= numberOfIterations/plansFileInterval; i++) {
				//String plansFile = "D:/Workspace/data/cemdapMatsimCadyts/output/" + runId + "/ITERS/it." + i * plansFileInterval
				String plansFile = directoryRoot + "/ITERS/it." + i * plansFileInterval
						+ "/" + runId + "." + i * plansFileInterval + ".plans.xml.gz";
				
				Config config = ConfigUtils.createConfig();
				Scenario scenario = ScenarioUtils.createScenario(config);
				PopulationReaderMatsimV5 reader = new PopulationReaderMatsimV5(scenario);
				reader.readFile(plansFile);
				Population population = scenario.getPopulation();
	
				countSelectedPlanTypes(population, i * plansFileInterval);
			}
		}
		
		if (useOutputPlans == true) {
			String plansFileOutput = "D:/Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/" + runId + ".output_plans.xml.gz";
			//String plansFileOutput = "D:/Workspace/data/cemdapMatsimCadyts/output/" + runId + "/" + runId + ".output_plans.xml.gz";
			
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
			PopulationReaderMatsimV5 reader = new PopulationReaderMatsimV5(scenario);
			reader.readFile(plansFileOutput);
			Population population = scenario.getPopulation();
	
			countSelectedPlanTypes (population, 99999);
		}
		
		writeFile();
	}
	
	
	static void countSelectedPlanTypes (Population population, int iteration) {
		int counterStayHomePlans = 0;
		int counterOtherPlans = 0;
		int counterCarPlans = 0;
		int counterPtPlans = 0;
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
						LegImpl leg = (LegImpl) selectedPlan.getPlanElements().get(i);
						
						mode = leg.getMode();
						
						if (mode == "car") {
							counterCarPlans++;
						} else if (mode == "pt") {
							counterPtPlans++;
						} else if (mode == "walk") {
							counterWalkPlans++;
						} else {
							throw new RuntimeException("In current implementation leg mode must either be car, pt, or walk");
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
		walkPlansMap.put(iteration, counterWalkPlans);
	}

	
	static void writeFile() {
		new File(directoryRoot + "/analysis").mkdir();
		BufferedWriter bufferedWriter = null;
			
		try {
			File output = new File(directoryRoot + "/analysis/selectedPlans.txt");
			FileWriter fileWriter = new FileWriter(output);
			bufferedWriter = new BufferedWriter(fileWriter);
			
			// Header
			bufferedWriter.write("It." + "\t" + "stayHomePlans" + "\t" + "otherPlans" + "\t" 
					+ "carPlans" + "\t" + "ptPlans" + "\t" + "walkPlans");
			bufferedWriter.newLine();
			
			for (int iteration : stayHomePlansMap.keySet()) {
    			bufferedWriter.write(iteration + "\t" + stayHomePlansMap.get(iteration) + "\t" + otherPlansMap.get(iteration) + "\t" 
    					+ carPlansMap.get(iteration) + "\t" + ptPlansMap.get(iteration) + "\t" + walkPlansMap.get(iteration));
    			bufferedWriter.newLine();
    		}    		
	    } catch (FileNotFoundException ex) {
	        ex.printStackTrace();
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
		System.out.println("Analysis file " + directoryRoot + "/analysis/selectedPlans.txt" + " written.");
	}
}