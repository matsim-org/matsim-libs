package playground.mmoyo.utils;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**Reads many populations and merges them adding a index to repeated persons**/ 
public class PlansMerger {
	private static final Logger log = Logger.getLogger(PlansMerger.class);

	private String netFilePath;
	
	public void setNetFilePath(String netFilePath){
		this.netFilePath= netFilePath;
	}
	
	private Population[] loadPopArrayFromConfig (String[] configs){
		Population[] populationArray = new Population[configs.length];
		for (int i=0; i<configs.length; i++){
			Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configs[i]));
			populationArray[i] = scenario.getPopulation();
		}
		return populationArray;
	}

	private Population[] loadPopArrayfromPopFiles(final String[] populationsFiles){
		Population[] popArray = new Population[populationsFiles.length];
		           
		DataLoader dLoader = new DataLoader();
		//PlanScoreRemover planScoreRemover = new PlanScoreRemover();
		//NonSelectedPlansRemover nonSelectedPlansRemover = new NonSelectedPlansRemover();
		for (int i=0; i<populationsFiles.length; i++){
			Scenario scn = dLoader.readNetwork_Population(this.netFilePath, populationsFiles[i]);
			Population pop = scn.getPopulation();
			//planScoreRemover.run(pop);
			//nonSelectedPlansRemover.run(pop);
			popArray[i]= pop;
		}
		dLoader= null;
		//planScoreRemover = null;
		//nonSelectedPlansRemover = null;
		return popArray;
	}	
	
	
	/**args are the config files containing the populations to merge, 
	 * they may have the same persond id's, a suffix is added*/
	public Population agentAggregator(String[] configs){
		int popsNum = configs.length; 
		Population newPopulation = new PopulationImpl((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		Population[] populationArray = loadPopArrayFromConfig (configs);
		
		for (Person person : populationArray[0].getPersons().values()) {
			Id id = person.getId();
			Person[] personArray= new Person[popsNum];

			byte noNull = 0;
			for (int i=0; i<popsNum; i++){
				personArray[i]= populationArray[i].getPersons().get(id);
				if (personArray[i]!=null) noNull++; 
			}
			
			if (noNull== popsNum ){
				for (byte i=0; i<popsNum; i++){
					personArray[i].setId(new IdImpl(id.toString() + (i+1)));
					newPopulation.addPerson(personArray[i]);
				}
			}
		}
	
		return newPopulation;
	}

	/**Assuming that the given populations do not share any agent. If yes, an error is shown and*/
	public Population diffAgentMerger (String[] popArray){
		Population newPopulation = new PopulationImpl((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		String strError = "The agent is repeated in populations: ";

		Population[] pops =  loadPopArrayfromPopFiles(popArray);
		
		for (int i=0; i<pops.length; i++){
			Population pop = pops[i];
			for (Person person : pop.getPersons().values()) {
				if (!newPopulation.getPersons().containsKey(person.getId())){
					newPopulation.addPerson(person);  	
				}else{
					log.warn(strError + person.getId());
				}
			}
		}
		return newPopulation;
	}

	public Population plansAggregator (String[] popArray){
		return plansAggregator(loadPopArrayfromPopFiles(popArray));	
	}
	
	
	/**
	 * This method merges the population by adding repeated agents as new plans (it considers only selected plans) 
	 */
	public Population plansAggregator (final Population [] popArray){
		Population popBase = popArray[0];
		
		for (int i=1; i<popArray.length; i++){
			Population pop_i = popArray[i];
			for (Person person_i : pop_i.getPersons().values()) {
				Person person;
				if (popBase.getPersons().containsKey(person_i.getId())){
					person = popBase.getPersons().get(person_i.getId());
					person.addPlan(person_i.getSelectedPlan());
				}else{
					popBase.addPerson(person_i);
				}

			}
		}
		return popBase;
	}
	
	public static void main(String[] args) {
		String [] popFilePathArray = new String[3];
		String netFilePath;
		if (args.length>0){
			popFilePathArray[0]= args[0];
			popFilePathArray[1]= args[1];
			popFilePathArray[2]=  args[2];
			netFilePath = args[3];
		}else{
			popFilePathArray[0]="../../input/juni/overEstimatedDemandPlans.xml.gz";
			popFilePathArray[1]="../../input/juni/output/overEstimatedDemandPlans.xml.gz";
			popFilePathArray[2]="../../input/juni/output/overEstimatedDemandPlans.xml.gz";
			netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		}
			
		PlansMerger plansMerger = new PlansMerger();
		plansMerger.setNetFilePath(netFilePath);
		Population mergedPop = plansMerger.plansAggregator(popFilePathArray);
		
		//write population in same Directory
		String outputFile = new File(popFilePathArray[0]).getParent() + File.separatorChar + "mergedPlans.xml.gz";		
		
		Network net = new DataLoader().readNetwork(netFilePath);
		PopulationWriter popWriter= new PopulationWriter(mergedPop, net);
		popWriter.write(outputFile);	
	
		//write sample in first pop directory
		popWriter = new PopulationWriter(new FirstPersonsExtractor().run(mergedPop, 2), net);
		File file = new File(popFilePathArray[0]);
		popWriter.write(file.getParent() + "/planSample.xml") ;
		System.out.println("done");
		
	}
}