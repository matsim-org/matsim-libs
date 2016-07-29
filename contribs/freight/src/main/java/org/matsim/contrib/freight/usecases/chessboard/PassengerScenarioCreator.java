package org.matsim.contrib.freight.usecases.chessboard;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;

public class PassengerScenarioCreator {
	
	static int agentCounter = 1;
	
	static int nuOfAgentsPerHomeLink = 1;
	
	public static void main(String[] args) {
		
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("input/usecases/chessboard/network/grid9x9.xml");
		
		Population population = scenario.getPopulation(); 
		
		for(int i=1;i<10;i++){
			Id<Link> homeId = Id.create("i("+i+",9)R", Link.class);
			Id<Link> workId = Id.create("i("+i+",0)", Link.class);
			List<Person> persons = createPersons(homeId,workId,scenario);
			for(Person p : persons) population.addPerson(p);

			Id<Link> homeIdR = Id.create("i("+i+",0)", Link.class);
			Id<Link> workIdR = Id.create("i("+i+",9)R", Link.class);
			List<Person> personsR = createPersons(homeIdR,workIdR,scenario);
			for(Person p : personsR) population.addPerson(p);
		}
		
		for(int i=1;i<10;i++){
			Id<Link> homeId = Id.create("j(0,"+i+")R", Link.class);
			Id<Link> workId = Id.create("j(9,"+i+")", Link.class);
			List<Person> persons = createPersons(homeId,workId,scenario);
			for(Person p : persons) population.addPerson(p);
			
			Id<Link> homeIdR = Id.create("j(9,"+i+")", Link.class);
			Id<Link> workIdR = Id.create("j(0,"+i+")R", Link.class);
			List<Person> personsR = createPersons(homeIdR,workIdR,scenario);
			for(Person p : personsR) population.addPerson(p);
		}
		
		new PopulationWriter(population, scenario.getNetwork()).write("input/usecases/chessboard/passenger/passengerPlansV2.xml");
	}

	private static List<Person> createPersons(Id<Link> homeId, Id<Link> workId,Scenario scenario) {
		LeastCostPathCalculator lcpa = new DijkstraFactory().createPathCalculator(scenario.getNetwork(), 
				new FreespeedTravelTimeAndDisutility(-1.0, -1.0, -1.0), new FreespeedTravelTimeAndDisutility(-1.0, -1.0, -1.0));
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		List<Person> persons = new ArrayList<Person>();
		for(int agent=0;agent<nuOfAgentsPerHomeLink;agent++){
			
			Person person = popFactory.createPerson(Id.create(agentCounter, Person.class));
			agentCounter++;
			Plan plan = popFactory.createPlan();
			plan.setPerson(person);

			Activity act1 = popFactory.createActivityFromLinkId("home", homeId);
			act1.setEndTime(8*60*60);
			plan.addActivity(act1);
			Leg leg1 = popFactory.createLeg(TransportMode.car);
			Path path1 = lcpa.calcLeastCostPath(scenario.getNetwork().getLinks().get(homeId).getToNode(), scenario.getNetwork().getLinks().get(workId).getFromNode(), act1.getEndTime(), person, null);
			LinkNetworkRouteImpl linkNetworkRoute = new LinkNetworkRouteImpl(homeId, getLinkIds(path1), workId);
			leg1.setRoute(linkNetworkRoute);
			plan.addLeg(leg1);

			Activity act2 = popFactory.createActivityFromLinkId("work", workId);
			act2.setMaximumDuration(8*60*60);
			plan.addActivity(act2);

			Leg leg2 = popFactory.createLeg(TransportMode.car);
			Path path2 = lcpa.calcLeastCostPath(scenario.getNetwork().getLinks().get(workId).getToNode(), scenario.getNetwork().getLinks().get(homeId).getFromNode(), act1.getEndTime(), person, null);
			LinkNetworkRouteImpl linkNetworkRoute2 = new LinkNetworkRouteImpl(workId, getLinkIds(path2), homeId);
			leg2.setRoute(linkNetworkRoute2);
			plan.addLeg(leg2);

			plan.addActivity(popFactory.createActivityFromLinkId("home", homeId));

			person.addPlan(plan);
			person.setSelectedPlan(plan);
			persons.add(person);
		}
		return persons;
	}

	private static List<Id<Link>> getLinkIds(Path path1) {
		List<Id<Link>> links = new ArrayList<Id<Link>>();
		for(Link l : path1.links){ links.add(l.getId()); }
		return links;
	}

}
