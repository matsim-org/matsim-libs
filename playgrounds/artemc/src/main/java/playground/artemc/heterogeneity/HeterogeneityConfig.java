package playground.artemc.heterogeneity;

import java.io.File;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class HeterogeneityConfig implements StartupListener,
ControlerListener {


	private static final Logger log = Logger.getLogger(HeterogeneityConfig.class);

	public static final String LAMBDA_INCOME = "lambda_income";
	public static final String LAMBDA_DISTANCE = "lambda_dist";
	final static String modName = "Heterogeneity";
	static double lambda_income = 1.0;
	static double lambda_dist = 1.0;
	static boolean heterogeneitySwitch = false;
	static boolean homogeneousIncomeFactorSwitch = false;


	private Controler controler;
	private Config config;
	private Scenario scenario;

	private String inputPath;
	private String simulationType;

	private HashMap<Id<Person>, Double> incomeFactors;

	public HeterogeneityConfig(String inputPath, Scenario scenario, String simulationType){
		this.inputPath = inputPath;
		this.scenario = scenario;
		this.simulationType = simulationType;

		//Initialize all maps
		this.incomeFactors = new HashMap<Id<Person>, Double>();
		for(Id<Person> personId:this.scenario.getPopulation().getPersons().keySet()){
			this.incomeFactors.put(personId, 1.0);
		}

	}

	@Override
	public void notifyStartup(StartupEvent event) {

		this.config = event.getControler().getConfig();

		//Initialize parameters
		String l_inc = config.getParam(modName, LAMBDA_INCOME);
		String l_dist = config.getParam(modName, LAMBDA_DISTANCE);

		log.info("Reading heterogeneity paramters... ");

		if (l_inc!= null){
			lambda_income = Double.parseDouble(l_inc);
			boolean fileProvided = new File(this.inputPath + "/incomes.xml").exists();
			if(fileProvided){
				this.loadIncomes();
				heterogeneitySwitch = true;	
			}
			else{
				log.error("Income heterogeneity parameter found, but NO INCOME FILE. Continuing with homogeneous user preference parameters...");
			}
		}

		if(!heterogeneitySwitch){
			log.error("No heterogeneity parameters found in config. Continuing with homogeneous user preference parameters...");
		}else{
			log.info("Heterogeneous user preferences successfully enabled!");
		}

	}

	private void loadIncomes() {

		log.info("loading income data from " + this.inputPath+"incomes.xml");
		new ObjectAttributesXmlReader(this.scenario.getPopulation().getPersonAttributes()).parse(this.inputPath+"incomes.xml");

		/*Calculate Income Statistics*/
		Integer sum=0;
		Double mean = 0.0;
		for(Id<Person> personId:this.scenario.getPopulation().getPersons().keySet()){
			sum = sum + (int) this.scenario.getPopulation().getPersonAttributes().getAttribute(personId.toString(), "income");
			mean = (double) sum / (double) this.scenario.getPopulation().getPersons().size();
		}

		
		/*Create map of personal income factors*/
		Double factorSum=0.0;
		Double factoreMean = 0.0;
		for(Id<Person> personId:this.scenario.getPopulation().getPersons().keySet()){
			Integer personIncome = (int) this.scenario.getPopulation().getPersonAttributes().getAttribute(personId.toString(), "income");
			double incomeFactor = Math.pow((double) personIncome/mean,this.getLambda_income());
			incomeFactors.put(personId, incomeFactor);
			factorSum = factorSum + incomeFactor;
		}
		
		/*For simulation with homogeneous parameters but adjusted for income factor mean*/
		if(simulationType.equals("homo")){
			log.info("Homogeneuos simulation with parameter adjustments for income factor mean is enabled...");
			factoreMean = factorSum / (double) incomeFactors.size();
			for(Id<Person> personId:incomeFactors.keySet()){
				incomeFactors.put(personId, factoreMean);
			}
		}
	}

	public double getLambda_income() {
		return lambda_income;
	}

	public double getLambda_dist() {
		return lambda_dist;
	}

	public HashMap<Id<Person>, Double> getIncomeFactors() {
		return incomeFactors;
	}

}
