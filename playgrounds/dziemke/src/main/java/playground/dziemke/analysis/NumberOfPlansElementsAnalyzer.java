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
public class NumberOfPlansElementsAnalyzer {
	// Parameters
	private static final  String runId = "run_162";
	private static final  int numberOfIterations = 100;
	
	// Input/output
	private static final  String directoryRoot = "../../../runs-svn/cemdapMatsimCadyts/" + runId;

	private static final  Map<Integer, Integer> stayHomePlansMap = new HashMap<Integer, Integer>();
	
	
	public static void main(String[] args) {
		
			
				String plansFile = directoryRoot + "/ITERS/it." + numberOfIterations
						+ "/" + runId + "." + numberOfIterations + ".plans.xml.gz";
				
				Config config = ConfigUtils.createConfig();
				Scenario scenario = ScenarioUtils.createScenario(config);
				PopulationReaderMatsimV5 reader = new PopulationReaderMatsimV5(scenario);
				reader.readFile(plansFile);
				Population population = scenario.getPopulation();
	
				countSelectedPlanTypes(population);
		
		
		
		writeFile();
	}
	
	
	static void countSelectedPlanTypes (Population population) {
		int counterStayHomePlans = 0;
		
		String mode;
		
		// iterate over persons
		for (Person person : population.getPersons().values()) {			
			Plan selectedPlan = person.getSelectedPlan();
			int numberOfPlanElements = selectedPlan.getPlanElements().size();
			
			if (!stayHomePlansMap.containsKey(numberOfPlanElements)) {
				stayHomePlansMap.put(numberOfPlanElements, 1);
			} else {
				int latestNumberOfPlansWithGivenNumberOfPlanElements = stayHomePlansMap.get(numberOfPlanElements);
				stayHomePlansMap.put(numberOfPlanElements, latestNumberOfPlansWithGivenNumberOfPlanElements + 1);
			}
				
				
			
		}
		
	}

	
	static void writeFile() {
		new File(directoryRoot + "/analysis").mkdir();
		BufferedWriter bufferedWriter = null;
			
		try {
			File output = new File(directoryRoot + "/analysis/numberOfPlanElements.txt");
			FileWriter fileWriter = new FileWriter(output);
			bufferedWriter = new BufferedWriter(fileWriter);
			
			// Header
			bufferedWriter.write("NumberOfPlanElements" + "\t" + "NumberOfPlansWithGivenNumberOfPlanElements");
			bufferedWriter.newLine();
			
			for (int numberOfPlanElements : stayHomePlansMap.keySet()) {
    			bufferedWriter.write(numberOfPlanElements + "\t" + stayHomePlansMap.get(numberOfPlanElements));
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
		System.out.println("Analysis file " + directoryRoot + "/analysis/numberOfPlanElements.txt" + " written.");
	}
}