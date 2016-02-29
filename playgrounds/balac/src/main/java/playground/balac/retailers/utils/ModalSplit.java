package playground.balac.retailers.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;


public class ModalSplit {
	final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/RetailersSummary");
	private int numberOfFirstRetailer = 29;
	private int numberOfSecondRetailer = 17;
	MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	PopulationReader populationReader = new MatsimPopulationReader(scenario);
	MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
	public void run(String plansFilePath, String networkFilePath) throws IOException {
		populationReader.readFile(plansFilePath);
		networkReader.readFile(networkFilePath);

		ArrayList<Id<ActivityFacility>> a = new ArrayList<>();
		int count = 0;
		int countCar = 0;
		int countBike = 0;
		int countWalk = 0;
		int countPt = 0;
		double centerX = 683217.0; 
		double centerY = 247300.0;		
		readLink.readLine();
		
			for(int i = 0; i < numberOfFirstRetailer; i++) {
			
				String s = readLink.readLine();
				String[] arr = s.split("\t");
				a.add(Id.create(arr[1], ActivityFacility.class));
				//scenario_new.getActivityFacilities().getFacilities().put(Id.create(arr[1]), f);
				
			}
			
			for(int i = 0; i < numberOfSecondRetailer; i++) {
				
				String s = readLink.readLine();
				String[] arr = s.split("\t");
				a.add(Id.create(arr[1], ActivityFacility.class));
			}
			double distance = 0.0;
		Population pop = scenario.getPopulation();	
		for (Person p:pop.getPersons().values()) {
			Leg previousLeg = null;
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Leg) {
					previousLeg = (Leg) pe;
					
				}
				else if (pe instanceof Activity) {
					
					if (((Activity) pe).getType().equals( "shopgrocery" ) && (Math.sqrt(Math.pow(((Activity) pe).getCoord().getX() - centerX, 2) + (Math.pow(((Activity) pe).getCoord().getY() - centerY, 2))) <= 5000))// && a.contains(((Activity) pe).getFacilityId())) 
						
						if (previousLeg.getMode().equals( "car" )) {
							distance+= RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) previousLeg.getRoute(), scenario.getNetwork());
							countCar++;
							count++;
						}
						else if (previousLeg.getMode().equals( "bike" )) {
							countBike++;
							count++;
						}
						else if (previousLeg.getMode().equals( "walk" )) {
							countWalk++;
							count++;
						}
						else if (previousLeg.getMode().equals( "pt" )) {
							countPt++;
							count++;
						}
						
					}
				}
				
			}
			
		
		System.out.println(count);
		System.out.println(countCar);
		System.out.println(countBike);
		System.out.println(countWalk);
		System.out.println(countPt);
		System.out.println(distance/countCar);
	}
	
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		ModalSplit m = new ModalSplit();
		m.run(args[0], args[1]);
	}

}
