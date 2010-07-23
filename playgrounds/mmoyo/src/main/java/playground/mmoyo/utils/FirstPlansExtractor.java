package playground.mmoyo.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.api.core.v01.Id;
import playground.mmoyo.Validators.PlanValidator;
import java.util.Arrays;

/**Reads a plan file, selects the first plans and creates a new plan file with them*/ 
public class FirstPlansExtractor {
	final int agentNum=100;
	private boolean selectPlan_act = true;   //select only plans whose first and last activities are the same type.
	private boolean selectBuslines = false;  //select only plans with pt legs that have the mentioned transit routes.

	public Population run(Population pop){
		ScenarioImpl tempScenario =new ScenarioImpl();
		PopulationImpl outputPopulation = new PopulationImpl(tempScenario);
		
		if(selectBuslines){
			String trLineId = "B-M44";
			String trLineId2 = "B-344";
			Id[] array = {new IdImpl(trLineId),new IdImpl(trLineId2)};
			List <Id> trList  = Arrays.asList(array);
			new TrRouteFilter4Plan().filterPlan(pop,trList);
		}
		
		if (selectPlan_act){
			new PlanValidator().delDiffTypeActs(pop); 
		}
		
		Collection<? extends Person> personColl= pop.getPersons().values();
		for (Iterator<? extends Person> iter = personColl.iterator(); iter.hasNext() && outputPopulation.getPersons().size()< agentNum;) {
			Person person = iter.next();	
			outputPopulation.addPerson(person);
		}
		
		System.out.println("number of agents " + outputPopulation.getPersons().size());
		return outputPopulation;
	}

	public static void main(String[] args) {
		String configFile;
		
		if (args.length==1){
			configFile= args[0];
		}else{
			configFile= "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/bestValues_config.xml";
		}
		
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = scenarioLoader.getScenario();
		scenarioLoader.loadScenario();
		
		System.out.println(scenario.getConfig().plans().getInputFile());
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(new FirstPlansExtractor().run(scenario.getPopulation()),scenario.getNetwork());
		popwriter.write(scenario.getConfig().controler().getOutputDirectory() + "Berlin5x_onlyCorrectActs.xml") ;
		System.out.println("done");
	}
}