package playground.mmoyo.utils;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mmoyo.Validators.PlanValidator;

/**Reads a population file, selects the first persons and creates a new population file with them*/ 
public class FirstPersonsExtractor {
	private boolean selectPlan_act = true;   //select only plans whose first and last activities are the same type.
	private boolean selectBuslines = false;  //select only plans with pt legs that have the mentioned transit routes.

	public Population run(final Population pop, int agentNum){
		ScenarioImpl tempScenario =(ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
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
		String netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String popFilePath = "../../input/basePlan5x/Baseplan_5x_36119agents.xml.gz";
		final int agentNum=4;
		
		DataLoader dataLoader = new DataLoader();
		final Population pop = dataLoader.readPopulation(popFilePath);
		Network net = dataLoader.readNetwork(netFilePath);
		
		Population popSample = new FirstPersonsExtractor().run(pop, agentNum);
		
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(popSample, net);
		File file = new File(popFilePath);
		popwriter.write(file.getParent() + "/4planSample.xml") ;
		System.out.println("done");
	}
}