package playground.gregor.scenariogen.hybrid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.apache.log4j.Logger;
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
import org.matsim.core.utils.misc.StringUtils;

public class PopulationGenerator90deg {

private static final Logger log = Logger.getLogger(PopulationGenerator90deg.class);
	
	public static void createPopulation(Scenario sc) {
		Network network = sc.getNetwork();
		ArrayList <Link> initLinks = new ArrayList<Link>();
		ArrayList <Link> destinationLinks = new ArrayList<Link>();
		for (Node node : network.getNodes().values()){
			if (isOriginNode(node)){
				initLinks.add(node.getOutLinks().values().iterator().next());
			}
			if (isDestinationNode(node)){
				destinationLinks.add(node.getOutLinks().values().iterator().next());
			}
		}

		Population population = sc.getPopulation();
		population.getPersons().clear();
		PopulationFactory factory = population.getFactory();


		//ugly coded - needs to be cleaned up [GL Mar 2015]
		Link topDown = initLinks.get(3);
		Link leftRightLink = initLinks.get(0);
		

		List<Double> topDownDepartureTimes = new ArrayList<>();
		List<Double> leftRightDepartureTimes = new ArrayList<>();
		try {
			loadDepartureTimes(topDownDepartureTimes,leftRightDepartureTimes,sc);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		for (double time : topDownDepartureTimes) {
			Person pers = factory.createPerson(Id.create("p"+population.getPersons().size(),Person.class));
			Plan plan = factory.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = factory.createActivityFromLinkId("origin", topDown.getId());
			act0.setEndTime(time);
			plan.addActivity(act0);
			Leg leg = factory.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = factory.createActivityFromLinkId("destination", getDestinationLinkId(topDown,destinationLinks));
			plan.addActivity(act1);
			population.addPerson(pers);
		}
		for (double time : leftRightDepartureTimes) {
			Person pers = factory.createPerson(Id.create("p"+population.getPersons().size(),Person.class));
			Plan plan = factory.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = factory.createActivityFromLinkId("origin", leftRightLink.getId());
			act0.setEndTime(time);
			plan.addActivity(act0);
			Leg leg = factory.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = factory.createActivityFromLinkId("destination", getDestinationLinkId(leftRightLink,destinationLinks));
			plan.addActivity(act1);
			population.addPerson(pers);
		}
	}

	private static void loadDepartureTimes(List<Double> topDownDepartureTimes, List<Double> leftRightDepartureTimes,Scenario sc) throws IOException {
		String tF = Constants.INPTU_DIR + "/originalTrajectories90DegConverted.txt";
								
		
		BufferedReader br = new BufferedReader(new FileReader(new File(tF)));
		String l = br.readLine();
		Set<String> handled = new HashSet<>();
		while (l != null) {
			String[] expl = StringUtils.explode(l, ' ');
			if (expl.length == 5&& !l.startsWith("#")) {
				String id = expl[0];
				if (!handled.contains(id)) {
					handled.add(id);
					double time = Double.parseDouble(expl[1])/15; //15 fps
					double x = Double.parseDouble(expl[2]);
					if (x < -2) { //left-to-right
						leftRightDepartureTimes.add(time);
					} else {
						topDownDepartureTimes.add(time);
					}
				}
			}
			l = br.readLine();
		}
		br.close();
		log.info(topDownDepartureTimes.size() + " agents walking top-down and " + leftRightDepartureTimes.size() + " agents walking left-right");
	}

	private static Id<Link> getDestinationLinkId(Link originLink, ArrayList<Link> destinationLinks) {
		String originNodeId = originLink.getFromNode().getId().toString();
		if (originNodeId.equals("6")){
			return Id.createLinkId("5");
		}
		if (originNodeId.equals("12")){
			return Id.createLinkId("11");
		}
		if (originNodeId.equals("8")){
			return Id.createLinkId("0");
		}		
		if (originNodeId.equals("10")){
			return Id.createLinkId("6_rev");
		}			
		return null;
	}

	private static boolean isOriginNode(Node node) {
//		return node.getId().toString().endsWith("w")||node.getId().toString().endsWith("s");
		return node.getId().toString().equals("6")||node.getId().toString().equals("12")||node.getId().toString().equals("8")||node.getId().toString().equals("10");
	}
	
	private static boolean isDestinationNode(Node node) {
		return node.getId().toString().equals("6")||node.getId().toString().equals("12")||node.getId().toString().equals("8")||node.getId().toString().equals("10");
	}


	protected static void createCorridorPopulation(Scenario sc, int populationSize){
		Population population = sc.getPopulation();
		population.getPersons().clear();
		PopulationFactory factory = population.getFactory();
		double t = 0;
		double leftFlowProportion = 1.;
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
