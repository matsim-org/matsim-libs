package matsimConnector.scenarioGenerator;

import java.util.HashSet;
import java.util.Set;

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

public class PopulationGenerator {

	protected static void createPopulation(Scenario sc, int populationSize) {
		Network network = sc.getNetwork();
		Double x_min=null;
		Double y_min=null;
		Double x_max=null;
		Double y_max=null;
		boolean firstIt = true;
		for (Node node : network.getNodes().values()){
			if (firstIt){
				x_min = node.getCoord().getX();
				x_max = node.getCoord().getX();
				y_min = node.getCoord().getY();
				y_max = node.getCoord().getY();
				firstIt = false;
			}
			else if(node.getCoord().getY() < y_min)
				y_min = node.getCoord().getY();
			else if(node.getCoord().getY() > y_max)
				y_max = node.getCoord().getY();
			else if(node.getCoord().getX() < x_min)
				x_min = node.getCoord().getX();
			else if(node.getCoord().getX() < x_max)
				x_max = node.getCoord().getX();
		}
		
		Set <Link> initLinks = new HashSet<Link>();
		
		for (Node node : network.getNodes().values()){
			if (isInitLink(node, x_max, y_max, x_min, y_min)){
				initLinks.add((Link)node.getOutLinks().values().toArray()[0]);
			}
		}
		
		Population population = sc.getPopulation();
		population.getPersons().clear();
		PopulationFactory factory = population.getFactory();
		double t = 0;
		double leftFlowProportion = .5;
		//Iterator<Link> iterator = initLinks.iterator();
		//for (Link link : iterator){
			
		
			int limit = (int)(populationSize*leftFlowProportion);
			for (int i = 0; i < limit; i++) {
				Person pers = factory.createPerson(Id.create("b"+i,Person.class));
				Plan plan = factory.createPlan();
				pers.addPlan(plan);
				Activity act0;
				act0 = factory.createActivityFromLinkId("origin", Id.create("l0",Link.class));
				act0.setEndTime(t);
				plan.addActivity(act0);
				Leg leg = factory.createLeg("car");
				plan.addLeg(leg);
				Activity act1 = factory.createActivityFromLinkId("destination", Id.create("l3",Link.class));
				plan.addActivity(act1);
				population.addPerson(pers);
			}
			for (int i = limit; i < populationSize; i++) {
				Person pers = factory.createPerson(Id.create("a"+i,Person.class));
				Plan plan = factory.createPlan();
				pers.addPlan(plan);
				Activity act0;
				act0 = factory.createActivityFromLinkId("origin", Id.create("l3Rev",Link.class));
				act0.setEndTime(t);
				plan.addActivity(act0);
				Leg leg = factory.createLeg("car");
				plan.addLeg(leg);
				Activity act1 = factory.createActivityFromLinkId("destination", Id.create("l0Rev",Link.class));
				plan.addActivity(act1);
				population.addPerson(pers);
			}
		//}
	}

	private static boolean isInitLink(Node node, double x_max, double y_max, double x_min, double y_min) {
		return node.getCoord().getX() == x_max || node.getCoord().getX() == x_min || node.getCoord().getY() == y_max || node.getCoord().getY() == y_min;
	}
	
	protected static void createCorridorPopulation(Scenario sc, int populationSize){
		Population population = sc.getPopulation();
		population.getPersons().clear();
		PopulationFactory factory = population.getFactory();
		double t = 0;
		double leftFlowProportion = .5;
		int limit = (int)(populationSize*leftFlowProportion);
		for (int i = 0; i < limit; i++) {
			Person pers = factory.createPerson(Id.create("b"+i,Person.class));
			Plan plan = factory.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = factory.createActivityFromLinkId("origin", Id.create("l0",Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = factory.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = factory.createActivityFromLinkId("destination", Id.create("l3",Link.class));
			plan.addActivity(act1);
			population.addPerson(pers);
		}
		for (int i = limit; i < populationSize; i++) {
			Person pers = factory.createPerson(Id.create("a"+i,Person.class));
			Plan plan = factory.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = factory.createActivityFromLinkId("origin", Id.create("l3Rev",Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = factory.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = factory.createActivityFromLinkId("destination", Id.create("l0Rev",Link.class));
			plan.addActivity(act1);
			population.addPerson(pers);
		}
	}
}
