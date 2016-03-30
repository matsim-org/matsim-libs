package playground.balac.utils;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
public class UtilsAvignon {

	/**
	 * @param args
	 */
	MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	PopulationReader populationReader = new MatsimPopulationReader(scenario);
	MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
	double centerX = 683217.0;  //Belvue coordinates
	double centerY = 247300.0;
	public UtilsAvignon(String plansFilePath, String networkFilePath) {
		populationReader.readFile(plansFilePath);
		networkReader.readFile(networkFilePath);
		
	}
	
	
	public void distanceToFromShop() {
		
		double distanceTo = 0.0;
		double distanceFrom = 0.0;
		double previousDistance = 0.0;
		boolean shop = false;
		int number = 0;
		boolean car = false;

		for(Person p: scenario.getPopulation().getPersons().values()) {
			car =false;
			shop = false;
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals( "car" )) {
						car = true;
					previousDistance = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) ((Leg) pe).getRoute(), scenario.getNetwork());
					if (shop == true) {
						distanceFrom +=previousDistance;
					}
					else car = false;
						
					}

				}
				else if (pe instanceof Activity) {
						shop = false;
						if (((Activity) pe).getType().equals( "shopgrocery" )) {
							if (car == true) {
							distanceTo += previousDistance;
							number++;
							}

							shop = true;
						}
						else shop = false;
					
				}
				
			}
		}
		
		System.out.println(distanceTo/number + " " + distanceFrom/number);
		
		
	}
	
	public void travelTimeToFromShop() {
		
		double travelTimeTo = 0.0;
		double travelTimeFrom = 0.0;
		double previousTravelTime = 0.0;
		boolean shop = false;
		boolean car = false;
		int number = 0;
		for(Person p: scenario.getPopulation().getPersons().values()) {
			
			car =false;
			shop = false;
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("car")) {
						car = true;
					previousTravelTime = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) ((Leg) pe).getRoute(), scenario.getNetwork());
					if (shop == true) {
						travelTimeFrom +=previousTravelTime;
					}
					else car = false;
						
					}

				}
				else if (pe instanceof Activity) {
						shop = false;
						if (((Activity) pe).getType().equals( "shopgrocery" )) {
							if (car == true) {
								travelTimeTo += previousTravelTime;
							number++;
							}

							shop = true;
						}
						else shop = false;
					
				}
				
			}
		}
		
		System.out.println(travelTimeTo/number + " " + travelTimeFrom/number);
	}
	
	public void diffDistanceToFromShop() {
		
		double distanceToInside = 0.0;
		double distanceToOutside = 0.0;
		double distanceFromOutside = 0.0;
		double distanceFromInside = 0.0;
		double previousDistance = 0.0;
		boolean shopInside = false;
		boolean shopOutside = false;
		int numberToInside = 0;
		int numberToOutside = 0;
		
		int numberFromInside = 0;
		int numberFromOutside = 0;
		boolean car =false;

		for(Person p: scenario.getPopulation().getPersons().values()) {
			car =false;
			shopInside = false;
			shopOutside = false;
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("car")) {
						car = true;
						previousDistance = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) ((Leg) pe).getRoute(), scenario.getNetwork());
						if (shopInside == true) {
							distanceFromInside +=previousDistance;
							numberFromInside++;
						}
						else if (shopOutside == true){
							distanceFromOutside += previousDistance;
							numberFromOutside++;
						}
					}
					else 
						car = false;
						
					

				}
				else if (pe instanceof Activity) {
						
						if (((Activity) pe).getType().equals("shopgrocery")) {
							if (car == true) {
								if (Math.sqrt(Math.pow(((Activity) pe).getCoord().getX() - centerX, 2) +(Math.pow(((Activity) pe).getCoord().getY() - centerY, 2))) < 15000) {
									
									distanceToInside += previousDistance;
									numberToInside++;
									shopInside = true;
								}
								else {
									distanceToOutside += previousDistance;
									numberToOutside++;
									shopOutside = true;
									
								}
							}

							
						}
						else {
							shopInside = false;
							shopOutside = false;
						}
					
				}
				
			}
		}
		
		System.out.println(distanceToInside/numberToInside + " " + distanceToOutside/numberToOutside + " " +distanceFromInside/numberFromInside + " " + distanceFromOutside/numberFromOutside);
		
		
	}
	
	public void diffTravelTimeToFromShop() {
		
		double travelTimeToInside = 0.0;
		double travelTimeToOutside = 0.0;
		double travelTimeFromOutside = 0.0;
		double travelTimeFromInside = 0.0;
		double previousDistance = 0.0;
		boolean shopInside = false;
		boolean shopOutside = false;
		int numberToInside = 0;
		int numberToOutside = 0;
		
		int numberFromInside = 0;
		int numberFromOutside = 0;
		boolean car = false;
		
		
		for(Person p: scenario.getPopulation().getPersons().values()) {
			
			car =false;
			shopInside = false;
			shopOutside = false;
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("car")) {
						car = true;
						previousDistance = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) ((Leg) pe).getRoute(), scenario.getNetwork());
						if (shopInside == true) {
							travelTimeFromInside += previousDistance;
							numberFromInside++;
						}
						else if (shopOutside == true){
							travelTimeFromOutside += previousDistance;
							numberFromOutside++;
						}
					}
					else 
						car = false;
						
					

				}
				else if (pe instanceof Activity) {
						
						if (((Activity) pe).getType().equals( "shopgrocery" )) {
							if (car == true) {
								if (Math.sqrt(Math.pow(((Activity) pe).getCoord().getX() - centerX, 2) +(Math.pow(((Activity) pe).getCoord().getY() - centerY, 2))) < 15000) {
									
									travelTimeToInside += previousDistance;
									numberToInside++;
									shopInside = true;
								}
								else {
									travelTimeToOutside += previousDistance;
									numberToOutside++;
									shopOutside = true;
									
								}
							}

							
						}
						else {
							shopInside = false;
							shopOutside = false;
						}
					
				}
				
			}
		}
		
		System.out.println(travelTimeToInside/numberToInside + " " + travelTimeToOutside/numberToOutside + " " + travelTimeFromInside/numberFromInside + " " + travelTimeFromOutside/numberFromOutside);
		
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		UtilsAvignon ua = new UtilsAvignon(args[0], args[1]);
		ua.diffDistanceToFromShop();
		

	}

}
