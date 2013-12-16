package playground.anhorni.surprice.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.anhorni.surprice.DayConverter;
import playground.anhorni.surprice.Surprice;

public class TwoRunAnalyzer {
	
	private final static Logger log = Logger.getLogger(TwoRunAnalyzer.class);
	private String run0path;
	private String run1path;
	private String outPath;
	private String inPath;
	
	private ScenarioImpl scenario0 = null; 
	private ScenarioImpl scenario1 = null; 
	
	private SupriceBoxPlot boxPlotCS = new SupriceBoxPlot("Weekly CS", "Income", "CS", 0.0, 0.0);
	private SupriceBoxPlot boxPlotScoreDiff = new SupriceBoxPlot("Weekly ScoreDiff", "Income", "ScoreDiff", 0.0, 0.0);
	
	public static void main(String[] args) {
		if (args.length != 4) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}		
		TwoRunAnalyzer analyzer = new TwoRunAnalyzer();
		analyzer.init(args[0], args[1], args[2], args[3]);
		analyzer.run();
	}
	
	public void init(String run0path, String run1path, String inPath, String outPath) {
		this.run0path = run0path;
		this.run1path = run1path;
		this.inPath = inPath;
		this.outPath = outPath;
				
		this.scenario0 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		this.scenario1 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new File(this.outPath).mkdirs();
	}
	
	public void run() {
		
		ObjectAttributes weekScores0 = new ObjectAttributes();
		ObjectAttributes weekScores1 = new ObjectAttributes();
		
		for (String day : Surprice.days) {	
			String plansFilePath0 = run0path + "/" + day + "/" + day + ".output_plans.xml.gz";
			MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario0);
			populationReader.readFile(plansFilePath0);
			
			String plansFilePath1 = run1path + "/" + day + "/" + day + ".output_plans.xml.gz";
			populationReader = new MatsimPopulationReader(this.scenario1);
			populationReader.readFile(plansFilePath1);	
			
			
			for (Person person : this.scenario0.getPopulation().getPersons().values()) {								
				Plan bestPlan = person.getSelectedPlan();
				double bestScore = bestPlan.getScore();
				for (Plan plan : person.getPlans()) {
					if (plan.getScore() > bestScore) {
						bestPlan = plan;
						bestScore = bestPlan.getScore();
					}
				}
				weekScores0.putAttribute(person.getId().toString(), "score." + day, bestScore);
				
				bestPlan = null;
				bestScore = Double.MIN_VALUE;
				for (Plan plan : this.scenario1.getPopulation().getPersons().get(person.getId()).getPlans()) {
					if (plan.getScore() > bestScore) {
						bestPlan = plan;
						bestScore = bestPlan.getScore();
					}
				}	
				weekScores1.putAttribute(person.getId().toString(), "score." + day, bestScore);
			}
			this.scenario0.getPopulation().getPersons().clear();
			this.scenario1.getPopulation().getPersons().clear();
		}	
		ObjectAttributes preferences = new ObjectAttributes();
		ObjectAttributesXmlReader preferencesReader = new ObjectAttributesXmlReader(preferences);
		preferencesReader.parse(this.inPath + "/preferences.xml");
		
		ObjectAttributes incomes = new ObjectAttributes();
		ObjectAttributesXmlReader incomesReader = new ObjectAttributesXmlReader(incomes);
		incomesReader.parse(this.inPath + "/incomes.xml");
		
		this.writeConsumerSurplus(weekScores0, weekScores1, preferences, incomes);
			
		log.info("=================== Finished analyses ====================");
	}
	
	private void writeConsumerSurplus(ObjectAttributes weekScores0, ObjectAttributes weekScores1, ObjectAttributes preferences,
			ObjectAttributes incomes) {
		
		String plansFilePath0 = run0path + "/mon/" + "mon.output_plans.xml.gz";
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario0);
		populationReader.readFile(plansFilePath0);
					
		TreeMap<Integer, ArrayList<Double>> incomesPerCategory = new TreeMap<Integer, ArrayList<Double>>();
		TreeMap<Integer, ArrayList<Double>> scoreDiffPerCategory = new TreeMap<Integer, ArrayList<Double>>();
		
		for (int i = 0; i < 9; i++) {
			incomesPerCategory.put(i, new ArrayList<Double>());
			scoreDiffPerCategory.put(i, new ArrayList<Double>());
		}
		
		for (Person person : this.scenario0.getPopulation().getPersons().values()) {
			
			double weekCS = 0.0;
			double weekScoreDiff = 0.0;
			for (String day : Surprice.days) {
				double scoreDiff = (Double) weekScores1.getAttribute(person.getId().toString(), "score." + day)
						- (Double) weekScores0.getAttribute(person.getId().toString(), "score." + day);
				
				double consumerSurplus = scoreDiff / (Double) preferences.getAttribute(person.getId().toString(), "dudm");
				weekCS += consumerSurplus;	
				weekScoreDiff += scoreDiff;
			}
			double income = (Double)incomes.getAttribute(person.getId().toString(), "income") * 8.0;			
			incomesPerCategory.get((int)income).add(weekCS);
			scoreDiffPerCategory.get((int)income).add(weekScoreDiff);
		}
		
		for (int i = 0; i < 9; i++) {
			boxPlotCS.addValuesPerCategory(incomesPerCategory.get(i), i+"" , "CS");
			boxPlotScoreDiff.addValuesPerCategory(scoreDiffPerCategory.get(i), i+"" , "ScoreDiff");
		}
		this.boxPlotCS.createChart();
		this.boxPlotCS.saveAsPng(outPath + "/CS.png", 800, 600);
		
		this.boxPlotScoreDiff.createChart();
		this.boxPlotScoreDiff.saveAsPng(outPath + "/ScoreDiff.png", 800, 600);
		

	}
}
