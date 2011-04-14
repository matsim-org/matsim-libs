package playground.anhorni.PLOC.analysis.postprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class CompareScenarios {	
	public String outpath = "src/main/java/playground/anhorni/output/PLOC/zh/";
	private ScenarioImpl baseScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private List<String> paths = new Vector<String>();
	private String networkFilePath;
	private String facilitiesFilePath;
	
	private final static Logger log = Logger.getLogger(CompareScenarios.class);

	public static void main(String[] args) {
		CompareScenarios comparator = new CompareScenarios();
		comparator.run(args[0]);
		log.info("Comparisons finished ...");
	}
	
	private void readPathsList(String pathsFile) {
		try {
	          BufferedReader bufferedReader = new BufferedReader(new FileReader(pathsFile));
 
	          this.networkFilePath = bufferedReader.readLine();
	          this.facilitiesFilePath = bufferedReader.readLine();
	          
	          String line;
	          while ((line = bufferedReader.readLine()) != null) {
	        	  this.paths.add(line);
	          }
	          
	        } // end try
        catch (IOException e) {
        	e.printStackTrace();
        }	
	}
	
	private void init(String pathsFile) {
		new File(outpath).mkdirs();
		this.readPathsList(pathsFile);
		new MatsimNetworkReader(baseScenario).readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(baseScenario).readFile(facilitiesFilePath);
	}
		
	public void run(String pathsFile) {
		this.init(pathsFile);
		
		CompareScores scoreComparator = new CompareScores(this.outpath);
		
		scoreComparator.openScoresFile(this.outpath + "bestScores.txt");
		
		for (String path : this.paths) {
			log.info("reading: " + path);
			this.readPopulation(path);
			scoreComparator.handleScenario(this.baseScenario);
		}
		scoreComparator.closeScoresFile();
		scoreComparator.printScores();
		scoreComparator.compareBestScores(this.outpath + "bestScores.txt", 
				this.outpath + "bestScoresStandardDeviationsinPercent.txt");
		
		CompareDestinations destinationComparator = new CompareDestinations();
		for (String path : this.paths) {
			log.info("reading: " + path);
			this.readPopulation(path);
			destinationComparator.handleScenario(this.baseScenario);
		}
		DecimalFormat formatter = new DecimalFormat("0.0");
		log.info("Distances from center point: " + formatter.format(destinationComparator.evaluateScenarios()) + "[m]");
	}
	
	private void readPopulation(String populationFilePath) {
		this.baseScenario.setPopulation(new PopulationImpl(this.baseScenario));
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.baseScenario);
		populationReader.readFile(populationFilePath);
	}
	
	public static Plan getBestPlan(Person person) {
		double highestScore = Double.MIN_VALUE;
		Plan bestPlan = person.getSelectedPlan();
		
		for (Plan plan : person.getPlans()) {
			if (plan.getScore() > highestScore) bestPlan = plan;
		}
		return bestPlan;
 	}
}
