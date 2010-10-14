package playground.mmoyo.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
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

/**Reads many populations and merges them adding a index to repeated persons**/ 
public class PlansMerger {
	private static final Logger log = Logger.getLogger(PlansMerger.class);

	/**args are the config files containing the populations to merge, they may have the same persond id's, a suffix is added*/
	public void agentAggregator(String[] configs){
		int populationsNum= configs.length;
		Network net = null;
		Population[] populationArray = new Population[populationsNum];
		Population newPopulation = new PopulationImpl(new ScenarioImpl());
		 
		for (int i=0; i<populationsNum; i++){
			ScenarioLoader sl = new ScenarioLoaderFactoryImpl().createScenarioLoader(configs[i]);
			Scenario scenario = sl.loadScenario();
			populationArray[i] = scenario.getPopulation();
			net = scenario.getNetwork();
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
	
		String outputFile= "../playgrounds/mmoyo/output/mergedPopulation.xml"; 
		System.out.println("writing output plan file..." +  outputFile);
		PopulationWriter popwriter = new PopulationWriter(newPopulation, net);
		popwriter.write(outputFile) ;
		System.out.println("done");
	}

	//Assuming that the given populations do not share any agent
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
	
	//Assuming that all given plans share all their agents. The first occurrence will we defined as selected plans, the next ones are non selected plans
	//the first argument must be a config file with the base population and network
	//the next arguments are only the next population files
	public void plansAggregator (String[] configs){
		 
		DataLoader dataLoader = new DataLoader();
		Scenario scenario =  dataLoader.loadScenario(configs[0]);
		Population newPopulation = scenario.getPopulation();  //the first population is taken as basis, all their plan will remain/end up as selected
		
		for (int i=1; i<configs.length; i++){
			DataLoader dataLoader2 = new DataLoader();
			Population population2 = dataLoader2.readPopulation(configs[i]);
			
			for (Person person2 : population2.getPersons().values()) {
				if (newPopulation.getPersons().containsKey(person2.getId())){
					Person person = newPopulation.getPersons().get(person2.getId());
					Plan newPlan  = new PlanImpl(person); 
					newPlan.setScore(person2.getSelectedPlan().getScore());
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
					newPopulation.addPerson(person2);
				}
			}
		}
		
		//String outputFile = scenario.getConfig().controler().getOutputDirectory() + "mergedPlans.xml";		
		String outputFile = "../playgrounds/mmoyo/output/merge/mergedPlans.xml";
		System.out.println("writing output merged plan file..." +  outputFile);
		PopulationWriter popwriter = new PopulationWriter(newPopulation, scenario.getNetwork());
		popwriter.write(outputFile);
		System.out.println("done");
	}

	
	public static void main(String[] args) {
		String [] configs = new String[3];
		configs[0]="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		configs[1]="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/inputPlans/fastestRoutes_plan.xml.gz";
		configs[2]="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/inputPlans/minTransfersRoutes_plan.xml.gz";
		//new PlansMerger().agentAggregator(configs);
		//new PlansMerger().diffAgentMerger(configs);
		new PlansMerger().plansAggregator(configs);
	}
}


