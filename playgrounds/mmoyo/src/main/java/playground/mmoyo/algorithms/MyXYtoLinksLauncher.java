package playground.mmoyo.algorithms;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.XY2Links;

import playground.mmoyo.utils.DataLoader;

public class MyXYtoLinksLauncher extends AbstractPersonAlgorithm{
	final Network net;
	XY2Links xy2Links;
	
	public MyXYtoLinksLauncher(final Network network){
		this.net = network;
		xy2Links = new XY2Links((NetworkImpl)this.net);	
	}
	
	@Override
	public void run(Person person) {
		for(Plan plan: person.getPlans()){
			for(PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					act.setLinkId(null);
					act.setFacilityId(null);
				}
			}
		}
		xy2Links.run(person);
	}
	
	
	public static void main(String[] args) {
		String popFilePath;
		String netFilePath;
		if (args.length>0){
			popFilePath = args[0];
			netFilePath = args[1];
		}else{
			popFilePath = "../../input/juni/newDemand/cleanedPopulation.xml.gz";
			netFilePath = "../../input/juni/newDemand/mivNetWithSuffix.xml.gz";
		}

		DataLoader dataLoader = new DataLoader();
		Scenario scn = dataLoader.readNetwork_Population(netFilePath, popFilePath);
		new MyXYtoLinksLauncher(scn.getNetwork()).run(scn.getPopulation());
		
		//write this strange population in output
		System.out.println("writing output plan file...");
		new PopulationWriter(scn.getPopulation(), scn.getNetwork()).write( new File(popFilePath).getParent() + "/planswithXYlinks.xml.gz");
		System.out.println("Done");
	}

	
	
}
