package playground.anhorni.surprice.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.anhorni.surprice.Surprice;

public class TwoRunAnalyzer {
	
	private final static Logger log = Logger.getLogger(TwoRunAnalyzer.class);
	private String run0path;
	private String run1path;
	private String outPath;
	
	private ScenarioImpl scenario0 = null; 
	private ScenarioImpl scenario1 = null; 
	
	public static void main(String[] args) {
		if (args.length != 3) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}		
		TwoRunAnalyzer analyzer = new TwoRunAnalyzer();
		analyzer.init(args[0], args[1], args[2]);
		analyzer.run();
	}
	
	public void init(String run0path, String run1path, String outPath) {
		this.run0path = run0path;
		this.run1path = run1path;
		this.outPath = outPath;
				
		this.scenario0 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		this.scenario1 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
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
				weekScores0.putAttribute(person.getId().toString(), "score." + day, person.getSelectedPlan().getScore());
				
				
				weekScores1.putAttribute(person.getId().toString(), "score." + day, 
						this.scenario1.getPopulation().getPersons().get(person.getId()).getSelectedPlan().getScore());
			}		
		}	
		ObjectAttributes preferences = new ObjectAttributes();
		ObjectAttributesXmlReader preferencesReader = new ObjectAttributesXmlReader(preferences);
		preferencesReader.parse(run0path + "/preferences.xml");
		
		ObjectAttributes incomes = new ObjectAttributes();
		ObjectAttributesXmlReader incomesReader = new ObjectAttributesXmlReader(incomes);
		incomesReader.parse(run0path + "/incomes.xml");
		
		this.writeConsumerSurplus(weekScores0, weekScores1, preferences, incomes);
			
		log.info("=================== Finished analyses ====================");
	}
	
	private void writeConsumerSurplus(ObjectAttributes weekScores0, ObjectAttributes weekScores1, ObjectAttributes preferences,
			ObjectAttributes incomes) {
		
		double x[] = new double[this.scenario0.getPopulation().getPersons().size()];				
		double y[] = new double[this.scenario0.getPopulation().getPersons().size()];
			
		int cnt = 0;
		for (Person person : this.scenario0.getPopulation().getPersons().values()) {
			
			double weekCS = 0.0;
			for (String day : Surprice.days) {
				double scoreDiff = (Double) weekScores0.getAttribute(person.getId().toString(), "score." + day) -
						(Double) weekScores1.getAttribute(person.getId().toString(), "score." + day);
				
				double consumerSurplus = scoreDiff / (Double) preferences.getAttribute(person.getId().toString(), "dudm");
				weekCS += consumerSurplus;	
			}
			double income = (Double)incomes.getAttribute(person.getId().toString(), "income") * 8.0;
			x[cnt] = income;
			y[cnt] = weekCS;
			cnt++;
		}
		XYScatterChart chart = new XYScatterChart("", "income", "CS");
		chart.addSeries("", x, y);
		chart.saveAsPng(this.outPath + "/CS.png", 800, 600);		
	}
}
