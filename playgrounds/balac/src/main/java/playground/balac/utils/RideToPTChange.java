package playground.balac.utils;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class RideToPTChange {

	public void run(String plansFilePath, String networkFilePath, String facilitiesfilePath, String outputFilePath) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(plansFilePath);
		
				
		int size = scenario.getPopulation().getPersons().values().size();
		Object[] arr = scenario.getPopulation().getPersons().values().toArray();
		Network network = scenario.getNetwork();
		for (Person p: scenario.getPopulation().getPersons().values()) {
			
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("ride")) {
						
						double distance = CoordUtils.calcEuclideanDistance(network.getLinks().get(((Leg) pe).getRoute().getStartLinkId()).getCoord(), network.getLinks().get(((Leg) pe).getRoute().getEndLinkId()).getCoord());
						
						((Leg) pe).setMode("pt");
						GenericRouteImpl route = new GenericRouteImpl(((Leg) pe).getRoute().getStartLinkId(), ((Leg) pe).getRoute().getEndLinkId());
						route.setDistance(distance);
						route.setTravelTime(distance * 1.3 / 4.36);
						((Leg) pe).setRoute(route);
						
					}
					
				}
			}
		}
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4(outputFilePath + "/plans_1perc_noride.xml");		
		
	}
	
	public static void main(String[] args) {
		
		RideToPTChange cp = new RideToPTChange();
				
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		String facilitiesfilePath = args[2];
		String outputFolder = args[3];
		
		cp.run(plansFilePath, networkFilePath,facilitiesfilePath, outputFolder);
	}

}
