package playground.balac.retailers.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class GrocaryDistanceTravelled {

	public static void main(String[] args) throws IOException {
		
		ArrayList<IdImpl> retailers = new ArrayList<IdImpl>();
		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/Avignon/AvignonResults_1pc/retailersWithoutRepeatedLinks_MinDistCosts.txt");
		
		int numberOfFirstRetailer = 29;
		int numberOfSecondRetailer = 17;
		
		readLink.readLine();
		
		for(int i = 0; i < numberOfFirstRetailer; i++) {
		
			String s = readLink.readLine();
			String[] arr = s.split("\t");
			retailers.add(new IdImpl(arr[1]));
			
			
		}
		
		for(int i = 0; i < numberOfSecondRetailer; i++) {
			
			String s = readLink.readLine();
			String[] arr = s.split("\t");
			retailers.add(new IdImpl(arr[1]));
		}
		
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
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
						
						previousDistance = RouteUtils.calcDistance((NetworkRoute)(((Leg) pe).getRoute()), scenario.getNetwork());
						
					}
				}
				
			}
		
		}
		
		System.out.println((double)distance/(double)numberOfLegs);
	}

}
