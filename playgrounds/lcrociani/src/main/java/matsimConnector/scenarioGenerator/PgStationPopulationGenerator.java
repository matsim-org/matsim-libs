package matsimConnector.scenarioGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import matsimConnector.utility.Constants;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.MatsimRandom;

import pedCA.output.Log;

public class PgStationPopulationGenerator {
	
	protected static void createPopulation(Scenario sc, int populationSize) {
		Network network = sc.getNetwork();
		ArrayList <Link> initLinks = new ArrayList<Link>();
		ArrayList <Link> destinationLinks = new ArrayList<Link>();
		for (Node node : network.getNodes().values()){
			if (isOriginNode(node)){
				Link next = node.getOutLinks().values().iterator().next();
				initLinks.add(next);
				Log.log("originLink: "+next.getId().toString());
			}
			if(isDestinationNode(node)){
				Link next = node.getInLinks().values().iterator().next();
				destinationLinks.add(next);
				Log.log("destinationLink: "+next.getId().toString());
			}
		}
				
		Population population = sc.getPopulation();
		population.getPersons().clear();
		PopulationFactory factory = population.getFactory();
		
		
		double flowProportion = 1./initLinks.size();
		int generated = 0;
		float h = 1.5f;
		for (Link link : initLinks){
			int linkLimit = (int)(populationSize*flowProportion);
			for (int i = 0; i < linkLimit & generated<populationSize; i++) {
				
				double departureTime = (h*3600)+(MatsimRandom.getRandom().nextGaussian()*1800);
				if (departureTime < 0)
					departureTime = 0;
				Person pers = factory.createPerson(Id.create("p"+population.getPersons().size(),Person.class));
				Plan plan = factory.createPlan();
				pers.addPlan(plan);
				Activity act0;
				act0 = factory.createActivityFromLinkId("origin", link.getId());
				
				act0.setEndTime(departureTime);
				plan.addActivity(act0);
				Leg leg = factory.createLeg("car");
				leg.setDepartureTime(departureTime);
				plan.addLeg(leg);
				Activity act1 = factory.createActivityFromLinkId("destination", getDestinationLinkId(link,destinationLinks));
				plan.addActivity(act1);
				population.addPerson(pers);
				++generated;
			}
			h+=1.f;
		}
	}

	private static Id<Link> getDestinationLinkId(Link originLink, ArrayList<Link> destinationLinks) {
		String originNodeId = originLink.getFromNode().getId().toString();
		if (originNodeId.endsWith("s"))
			for (Link link : destinationLinks)
				if (link.getToNode().getId().toString().endsWith("w"))
					return link.getToNode().getInLinks().values().iterator().next().getId();
		if (originNodeId.endsWith("w"))
			for (Link link : destinationLinks)
				if (link.getToNode().getId().toString().endsWith("s"))
					return link.getToNode().getInLinks().values().iterator().next().getId();
		return null;
	}

	private static boolean isOriginNode(Node node) {
		return node.getId().toString().endsWith("s")||node.getId().toString().endsWith("w");
	}
	
	private static boolean isDestinationNode(Node node) {
		return node.getId().toString().endsWith("s")||node.getId().toString().endsWith("w");
	}	
}
