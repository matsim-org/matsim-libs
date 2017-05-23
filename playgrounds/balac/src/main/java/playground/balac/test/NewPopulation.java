package playground.balac.test;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class NewPopulation {

	public static void main(String[] args) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[1]);
		populationReader.readFile(args[0]);
		
		MutableScenario scenario2 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader2 = new MatsimNetworkReader(scenario2.getNetwork());
		networkReader2.readFile(args[1]);
		
		Network network = scenario.getNetwork();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			boolean first = true;
			boolean addingPT = false;
			Plan plan = person.getSelectedPlan();
			Person newPerson = scenario2.getPopulation().getFactory().createPerson(person.getId());
			
			Plan newPlan = scenario2.getPopulation().getFactory().createPlan();
			Link startLink=null;
			Link endLink=null;
			for (PlanElement pe : plan.getPlanElements()) {
				
				if (pe instanceof Activity) {
					
					if (!((Activity) pe).getType().equals("pt interaction")) {
						if (addingPT) {
							Leg newLeg = scenario2.getPopulation().getFactory().createLeg("pt");
							GenericRouteImpl route = new GenericRouteImpl(startLink.getId(), endLink.getId());
							double distance = CoordUtils.calcEuclideanDistance(startLink.getCoord(), endLink.getCoord());
							distance *= 1.3;
							route.setDistance(distance);
							route.setTravelTime(distance / 4.17);
							newLeg.setTravelTime(distance / 4.17);
							newLeg.setRoute(route);
							newPlan.addLeg(newLeg);
							addingPT = false;
							first = true;
						}
						newPlan.addActivity((Activity) pe);
						
					}
				}
				else {
					if (((Leg)pe).getMode().equals("transit_walk") && first) {
						addingPT = true;
						first = false;
						startLink = network.getLinks().get(((Leg)pe).getRoute().getStartLinkId());
						endLink = network.getLinks().get(((Leg)pe).getRoute().getEndLinkId());
						
					}
					else if (((Leg)pe).getMode().equals("transit_walk")) {
						endLink = network.getLinks().get(((Leg)pe).getRoute().getEndLinkId());

						
					}
					else if (!addingPT)
						newPlan.addLeg((Leg) pe);
				}
				
				
			}
			
			newPerson.addPlan(newPlan);
			newPerson.setSelectedPlan(newPlan);
			scenario2.getPopulation().addPerson(newPerson);
			
		}
		
		new PopulationWriter(scenario2.getPopulation(), 
				scenario2.getNetwork()).writeV4("C:\\Users\\balacm\\Desktop\\population_telept.xml.gz");	


	}

}
