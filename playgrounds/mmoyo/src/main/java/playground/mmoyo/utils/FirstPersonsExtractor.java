package playground.mmoyo.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import playground.mmoyo.Validators.PlanValidator;

/**Gets a population object and returns a sample population with first agents.
 * The difference with FirstPersonExtractorFromFile is that this class receives a population object, not a population file to read.
 * */ 
public class FirstPersonsExtractor {
	private boolean selectPlan_act = false;   //select only plans whose first and last activities are the same type.
	private boolean selectBuslines = false;  //select only plans with pt legs that have the mentioned transit routes.

	public Population run(final Population pop, int agentNum){
		ScenarioImpl tempScenario =(ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population outputPopulation = PopulationUtils.createPopulation(tempScenario.getConfig(), tempScenario.getNetwork());
		
		if(selectBuslines){
			String trLineId = "B-M44";
			String trLineId2 = "B-344";
			List<Id<TransitRoute>> trList = new ArrayList<>();
			trList.add(Id.create(trLineId, TransitRoute.class));
			trList.add(Id.create(trLineId2, TransitRoute.class));
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
		String netFilePath;
		String popFilePath;
		final int agentNum;	
		
		if (args.length>0){
			netFilePath = args[0];
			popFilePath = args[1];
			agentNum = Integer.valueOf (args[2]);
		}else{
			netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
			popFilePath = "../../input/NewTransitSchedule/routedPlan_walk10.0_dist0.0_tran240.0_wait0.0.xml.gz";
			agentNum=50;	
		}
		
		DataLoader dataLoader = new DataLoader();
		final Population pop = dataLoader.readPopulation(popFilePath);
		Network net = dataLoader.readNetwork(netFilePath);
		
		Population popSample = new FirstPersonsExtractor().run(pop, agentNum);
		
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(popSample, net);
		File file = new File(popFilePath);
		popwriter.write(file.getParent() + "/" + file.getName() + agentNum + "planSample.xml") ;
		System.out.println("done");
	}
}