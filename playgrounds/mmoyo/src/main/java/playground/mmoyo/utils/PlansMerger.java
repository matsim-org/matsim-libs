package playground.mmoyo.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.ScenarioLoaderFactoryImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PlanImpl;

import playground.mmoyo.utils.calibration.PlanScoreRemover;

/**Reads many populations and merges them adding a index to repeated persons**/ 
public class PlansMerger {
	private static final Logger log = Logger.getLogger(PlansMerger.class);

	/**args are the config files containing the populations to merge, 
	 * they may have the same persond id's, a suffix is added*/
	public Population agentAggregator(String[] configs){
		int populationsNum= configs.length;
		Population[] populationArray = new Population[populationsNum];
		Population newPopulation = new PopulationImpl(new ScenarioImpl());
		 
		for (int i=0; i<populationsNum; i++){
			ScenarioLoader sl = new ScenarioLoaderFactoryImpl().createScenarioLoader(configs[i]);
			Scenario scenario = sl.loadScenario();
			populationArray[i] = scenario.getPopulation();
		}
		
		for (Person person : populationArray[0].getPersons().values()) {
			Id id = person.getId();
			Person[] personArray= new Person[populationsNum];

			byte noNull = 0;
			for (int i=0; i<populationsNum; i++){
				personArray[i]= populationArray[i].getPersons().get(id);
				if (personArray[i]!=null) noNull++; 
			}
			
			if (noNull== populationsNum ){
				for (byte i=0; i<populationsNum; i++){
					personArray[i].setId(new IdImpl(id.toString() + (i+1)));
					newPopulation.addPerson(personArray[i]);
				}
			}
		}
	
		return newPopulation;
	}

	/**Assuming that the given populations do not share any agent*/
	public void diffAgentMerger (String[] configs){
		Scenario scenario = null;
		Population newPopulation = new PopulationImpl(new ScenarioImpl());
		 
		String warning = "The agent is repeated in populations: ";
		for (int i=0; i<configs.length; i++){
			DataLoader dataLoader = new DataLoader();
			scenario =  dataLoader.loadScenario(configs[i]);
			
			for (Person person : scenario.getPopulation().getPersons().values()) {
				if (!newPopulation.getPersons().containsKey(person.getId())){
					newPopulation.addPerson(person);  	
				}else{
					log.warn (warning + person.getId());
				}
			}
		}
	
		String outputFile= "../playgrounds/mmoyo/output/input/merge/output/mergedPopulation.xml"; 
		System.out.println("writing output plan file..." +  outputFile);
		PopulationWriter popwriter = new PopulationWriter(newPopulation, scenario.getNetwork());
		popwriter.write(outputFile);
		System.out.println("done");
	}
	
	
	/**
	 * This method merges the population by adding repeated agents as new plans (only selected plans) 
	 * 
	 */
	//Assuming that all given plans share all their agents. The first occurrence will we defined as selected plans, the next ones are non selected plans
	//the first argument must be a config file with the base population and network
	//the next arguments are only the next population files
	public Population plansAggregator (String[] populationsFiles){
		Population popBase = new DataLoader().readPopulation(populationsFiles[0]);
		
		new PlanScoreRemover().run(popBase);
		
		for (int i=1; i<populationsFiles.length; i++){
			DataLoader dataLoader2 = new DataLoader();
			Population population2 = dataLoader2.readPopulation(populationsFiles[i]);
			
			new PlanScoreRemover().run(population2);
			
			for (Person person2 : population2.getPersons().values()) {
				if (popBase.getPersons().containsKey(person2.getId())){
					Person person = popBase.getPersons().get(person2.getId());
					Plan newPlan  = new PlanImpl(person); 
					//newPlan.setScore(person2.getSelectedPlan().getScore());
					for (PlanElement pe : person2.getSelectedPlan().getPlanElements()){
						if (pe instanceof Activity){
							newPlan.addActivity((Activity)pe);		
						}else{
							newPlan.addLeg((Leg)pe);
						}
					}
					person.addPlan(newPlan);
					if (newPlan.isSelected()){
						System.out.println("this plan should not be selected");
					}
				}else{
					popBase.addPerson(person2);
				}
			}
		}
		return popBase;
		
	}
	
	public static void main(String[] args) {
		String [] configs = new String[3];
		configs[0]="";
		configs[1]="";
		configs[2]="";

		String config = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		Scenario scenario =   new DataLoader().loadScenario(config);
		String [] plansArray = new String[3];
		plansArray[0]="../playgrounds/mmoyo/output/merge/routedPlan_walk10.0_dist0.0_tran240.0.xml.gz";
		plansArray[1]="../playgrounds/mmoyo/output/merge/routedPlan_walk10.0_dist0.6_tran1020.0.xml.gz";
		Population mergedPop = new PlansMerger().plansAggregator(plansArray);
		
		//write population
		String outputFile = scenario.getConfig().controler().getOutputDirectory() + "mergedPlans.xml";		
		System.out.println("writing output merged plan file..." +  outputFile);
		PopulationWriter popwriter = new PopulationWriter(mergedPop, scenario.getNetwork());
		popwriter.write(outputFile);
		System.out.println("done");		
	}
}


