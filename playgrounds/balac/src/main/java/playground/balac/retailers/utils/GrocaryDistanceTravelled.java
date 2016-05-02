package playground.balac.retailers.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.retailers.data.Retailer;

public class GrocaryDistanceTravelled {

	public static void main(String[] args) throws IOException {
		
		ArrayList<Id<Retailer>> retailers = new ArrayList<>();
		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/Avignon/AvignonResults_1pc/retailersWithoutRepeatedLinks_MinDistCosts.txt");
		
		int numberOfFirstRetailer = 29;
		int numberOfSecondRetailer = 17;
		
		readLink.readLine();
		
		for(int i = 0; i < numberOfFirstRetailer; i++) {
		
			String s = readLink.readLine();
			String[] arr = s.split("\t");
			retailers.add(Id.create(arr[1], Retailer.class));
			
			
		}
		
		for(int i = 0; i < numberOfSecondRetailer; i++) {
			
			String s = readLink.readLine();
			String[] arr = s.split("\t");
			retailers.add(Id.create(arr[1], Retailer.class));
		}
		
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		double distance = 0;
		int numberOfLegs = 0;
		for(Person p:scenario.getPopulation().getPersons().values()) {
			
			Plan plan = p.getSelectedPlan();
			double previousDistance = 0;
			for (PlanElement pe: plan.getPlanElements()) {
				
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().equals("shopgrocery")) {
						distance += previousDistance;
						numberOfLegs++;
						
					}
				}
				else if (pe instanceof Leg) {
					
					if (((Leg) pe).getMode().equals("car")) {
						
						previousDistance = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute)(((Leg) pe).getRoute()), scenario.getNetwork());
						
					}
				}
				
			}
		
		}
		
		System.out.println(distance/numberOfLegs);
	}

}
