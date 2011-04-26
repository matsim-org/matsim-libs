package playground.anhorni.PLOC.analysis.postprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;

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
	public String outpath;
	private ScenarioImpl baseScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private int numberOfAnalyses;
	private String path;
	private String networkFilePath;
	private String facilitiesFilePath;
	private String plansFileName;
	
	private final static Logger log = Logger.getLogger(CompareScenarios.class);

	public static void main(String[] args) {
		CompareScenarios comparator = new CompareScenarios();
		comparator.run(args[0]);
		log.info("Comparisons finished ...");
	}
	
	private void readConfig(String configFile) {
		try {
	          BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile));
 
	          this.networkFilePath = bufferedReader.readLine();
	          this.facilitiesFilePath = bufferedReader.readLine();
	          this.numberOfAnalyses = Integer.parseInt(bufferedReader.readLine());
	          this.path = bufferedReader.readLine();
	          this.plansFileName = bufferedReader.readLine();
	          this.outpath = bufferedReader.readLine();
	        } // end try
        catch (IOException e) {
        	e.printStackTrace();
        }	
	}
	
	private void init(String configFile) {
		this.readConfig(configFile);
		new File(outpath).mkdirs();
		new MatsimNetworkReader(baseScenario).readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(baseScenario).readFile(facilitiesFilePath);
	}
		
	public void run(String pathsFile) {
		this.init(pathsFile);
		
		CompareScores scoreComparator = new CompareScores(this.outpath);
		scoreComparator.openScoresFile(this.outpath + "/scores.txt");
		
		for (int i = 0; i < this.numberOfAnalyses; i++) {
			String p = this.path + "/" + i + "/" + this.plansFileName;
			log.info("reading: " + p);
			this.readPopulation(p);
			scoreComparator.handleScenario(this.baseScenario);
		}
		scoreComparator.closeScoresFile();
		scoreComparator.printScores();
		
		scoreComparator.compareScores(this.outpath + "/scores.txt", 
				this.outpath + "/scoresStandardDeviationsInPercent.txt");
		
		CompareDestinations destinationComparator = new CompareDestinations();
		for (int i = 0; i < this.numberOfAnalyses; i++) {
			String p = this.path + "/" + i + "/" + this.plansFileName;
			log.info("reading: " + p);
			this.readPopulation(p);
			destinationComparator.handleScenario(this.baseScenario);
		}
		DecimalFormat formatter = new DecimalFormat("0.0000000");
		log.info("Distances from center point: " + formatter.format(destinationComparator.evaluateScenarios()) + "[m]");
		
		SimulatedLinkVolumesAndCounts volumesAnalyzer = new SimulatedLinkVolumesAndCounts(numberOfAnalyses, this.path, this.outpath, 
				this.baseScenario.getNetwork());
		volumesAnalyzer.run();		
	}
	
	private void readPopulation(String populationFilePath) {
		this.baseScenario.setPopulation(new PopulationImpl(this.baseScenario));
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.baseScenario);
		populationReader.readFile(populationFilePath);
	}
	
	public static Plan getBestPlan(Person person) {
		double highestScore = Double.MIN_VALUE;
		int bestPlanIndex = 0;
		
		int cnt = 0;
		for (Plan plan : person.getPlans()) {
			if (plan.getScore() > highestScore) {
				highestScore = plan.getScore();
				bestPlanIndex = cnt;
			}
			cnt++;
		}
		return person.getPlans().get(bestPlanIndex);
 	}
}
