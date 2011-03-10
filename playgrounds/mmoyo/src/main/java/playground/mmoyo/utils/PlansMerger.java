package playground.mmoyo.utils;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.ScenarioLoaderFactoryImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationImpl;

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
	 * This method merges the population by adding repeated agents as new plans (it considers only selected plans) 
	 */
	public Population plansAggregator (final Population [] popArray){
		Population popBase = popArray[0];
		
		for (int i=1; i<popArray.length; i++){
			Population population2 = popArray[i];
			for (Person person2 : population2.getPersons().values()) {
				Person person;
				if (popBase.getPersons().containsKey(person2.getId())){
					person = popBase.getPersons().get(person2.getId());
					person.addPlan(person2.getSelectedPlan());
				}else{
					popBase.addPerson(person2);
				}

			}
		}
		return popBase;
	}
	
	/**read an array of population files and return an array of Populations
	 * scores are removed, only non-selected plans are removed
	 * */
	public Population plansAggregator (final String[] populationsFiles){
		Population[] popArray = new Population[populationsFiles.length];
		           
		DataLoader dLoader = new DataLoader();
		PlanScoreRemover planScoreRemover = new PlanScoreRemover();
		NonSelectedPlansRemover nonSelectedPlansRemover = new NonSelectedPlansRemover();
		for (int i=0; i<populationsFiles.length; i++){
			Population pop = dLoader.readPopulation(populationsFiles[i]);
			planScoreRemover.run(pop);
			nonSelectedPlansRemover.run(pop);
			popArray[i]= pop;
		}
		dLoader= null;
		planScoreRemover = null;
		nonSelectedPlansRemover = null;
		return plansAggregator (popArray);
	}
	
	public static void main(String[] args) {
		String [] popFilePathArray = new String[3];
		popFilePathArray[0]="I:/z_Runs/";
		popFilePathArray[1]="I:/z_Runs/";
		popFilePathArray[2]="I:/z_Runs/";
		PlansMerger plansMerger = new PlansMerger();
		Population mergedPop = plansMerger.plansAggregator(popFilePathArray);
		
		//write population in same Directory
		String outputFile = new File(popFilePathArray[0]).getParent() + File.separatorChar + "mergedPlans.xml";		
		System.out.println("writing output merged plan file..." +  outputFile);
		String netFilePath = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		NetworkImpl net =   new DataLoader().readNetwork(netFilePath);
		new PopulationWriter(mergedPop, net).write(outputFile);
		System.out.println("done");		
	}
}


