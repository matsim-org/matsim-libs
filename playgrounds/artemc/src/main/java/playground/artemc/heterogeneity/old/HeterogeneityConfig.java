package playground.artemc.heterogeneity.old;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import org.matsim.core.gbl.MatsimRandom;

public class HeterogeneityConfig implements StartupListener,
ControlerListener {


	private static final Logger log = Logger.getLogger(HeterogeneityConfig.class);

	public static final String LAMBDA_INCOME = "incomeOnTravelCostLambda";
	public static final String LAMBDA_DISTANCE = "lambdaDistanceTravelTime";
	final static String modName = "heterogeneity";
	static double lambda_income = 1.0;
	static double lambda_dist = 1.0;
	static boolean heterogeneitySwitch = false;
	static boolean homogeneousIncomeFactorSwitch = false;
	static double heterogeneityFactor = 1.0;
	

	private Controler controler;
	private Config config;
	private Scenario scenario;

	private final String inputPath;
	private final String simulationType;



	private HashMap<Id<Person>, Double> incomeFactors;
	private HashMap<Id<Person>, Double> betaFactors;

	public HeterogeneityConfig(String inputPath, Scenario scenario, String simulationType, Double heterogeneityFactor){
		this.inputPath = inputPath;
		this.scenario = scenario;
		this.simulationType = simulationType;
		this.heterogeneityFactor = heterogeneityFactor;

		//Initialize all maps
		this.incomeFactors = new HashMap<Id<Person>, Double>();
		this.betaFactors = new HashMap<Id<Person>, Double>();
		for(Id<Person> personId:this.scenario.getPopulation().getPersons().keySet()){
			this.incomeFactors.put(personId, 1.0);
			this.betaFactors.put(personId, 1.0);
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
			this.lambda_income = Double.parseDouble(l_inc);
			boolean fileProvided = new File(this.inputPath + "/incomes.xml").exists();
			if(fileProvided){
				this.loadIncomes();
				heterogeneitySwitch = true;	
				if(simulationType.equals("heteroAlphaProp"))
					this.writeBetaFactors(this.betaFactors);

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

			if(simulationType.equals("heteroAlphaProp")){
				double randomFactor= 0.0;
				do{
					randomFactor = (MatsimRandom.getRandom().nextGaussian() * 0.2) + 1;
				}while(randomFactor <0 && randomFactor >2);
				System.out.println();
				betaFactors.put(personId, randomFactor);
			}


		}
		mean = (double) sum / (double) this.scenario.getPopulation().getPersons().size();

		/*Create map of personal income factors*/
		Double factorSum=0.0;
		Double factoreMean = 0.0;
		for(Id<Person> personId:this.scenario.getPopulation().getPersons().keySet()){
			Integer personIncome = (int) this.scenario.getPopulation().getPersonAttributes().getAttribute(personId.toString(), "income");
			double incomeFactor = Math.pow((double) personIncome/mean,(this.getLambda_income()*this.heterogeneityFactor));
			incomeFactors.put(personId, incomeFactor);
			factorSum = factorSum + incomeFactor;
		}

		//		It is more accurate to adjust the parameters in case of heterogeneous simulation, so that the mean still corresponds the original value -artemc nov '14		
		//		/*For simulation with homogeneous parameters but adjusted for income factor mean*/
		//		if(simulationType.equals("homo")){
		//			log.info("Homogeneuos simulation with parameter adjustments for income factor mean is enabled...");
		//			factoreMean = factorSum / (double) incomeFactors.size();
		//			for(Id<Person> personId:incomeFactors.keySet()){
		//				incomeFactors.put(personId, factoreMean);
		//			}
		//		}
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

	public String getSimulationType() {
		return simulationType;
	}
	
	public HashMap<Id<Person>, Double> getBetaFactors() {
		return betaFactors;
	}


	public void writeBetaFactors(HashMap<Id<Person>, Double> map){
		String filePath = this.scenario.getConfig().controler().getOutputDirectory() + "/betaNormalFactors.csv";
		File file = new File(filePath);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write("PersonId;BetaFactor;");
			writer.newLine();
			for (Id<Person> personId:map.keySet()){
				writer.write(personId.toString()+";"+map.get(personId).toString());
				writer.newLine();
			}
			writer.close();
			log.info("Schedule dealy early (beta) factors were written to " + filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



}
