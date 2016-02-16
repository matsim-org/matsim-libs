package playground.balac.utils;


import org.matsim.api.core.v01.Id;
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
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;

public class UtilsRetailers {

	/**
	 * @param args
	 */
	
	int numberOfRet1 = 29;
	int numberOfRet2 = 17;
	MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	PopulationReader populationReader = new MatsimPopulationReader(scenario);
	MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
	
	MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(scenario);
	//final BufferedReader inLink;
	
	public UtilsRetailers(String plansFilePath, String facilitiesFilePath, String networkFilePath) {
		
		networkReader.readFile(networkFilePath);
		populationReader.readFile(plansFilePath);
		
		facilitiesReader.readFile(facilitiesFilePath);
		
		//inLink = IOUtils.getBufferedReader(retailersSummaryFilePath);		
		
		
	}
	
	
	public void distacesToFromShops() {
		
		
		double distanceTocar = 0.0;
		double distanceFromcar = 0.0;
		double distanceTowalk = 0.0;
		double distanceFromwalk = 0.0;
		double distanceTobike = 0.0;
		double distanceFrombike = 0.0;
		double distanceTopt = 0.0;
		double distanceFrompt = 0.0;
		double previousDistance = 0.0;

		boolean shop = false;
		int numberTo = 0;
		int numberFrom = 0;

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
							distanceFromcar += previousDistance;
							numberFrom++;
						}
					
						
					}
					else car = false;
				}
				else if (pe instanceof Activity) {
						shop = false;
						if (((Activity) pe).getType().equals( "shopgrocery" ) && scenario.getActivityFacilities().getFacilities().containsKey(((Activity) pe).getFacilityId())) {
							if (car == true) {
							distanceTocar += previousDistance;
							numberTo++;
							}

							shop = true;
						}
						else shop = false;
					
				}
				
			}
		}
		System.out.println(distanceTocar/numberTo + " " + distanceFromcar/numberFrom);
		System.out.println(numberTo + " " + numberFrom);
		 numberTo = 0;
		 numberFrom = 0;
		 
		for(Person p: scenario.getPopulation().getPersons().values()) {
			car =false;
			shop = false;
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("pt") ) {
						car = true;
						previousDistance = ((Leg) pe).getRoute().getDistance();
						if (shop == true) {
							distanceFrompt += previousDistance;
							numberFrom++;
						}
					
						
					}
					else car = false;
				}
				else if (pe instanceof Activity) {
						shop = false;
						if (((Activity) pe).getType().equals( "shopgrocery" ) && scenario.getActivityFacilities().getFacilities().containsKey(((Activity) pe).getFacilityId())) {
							if (car == true) {
							distanceTopt += previousDistance;
							numberTo++;
							}

							shop = true;
						}
						else shop = false;
					
				}
				
			}
		}
		System.out.println(distanceTopt/numberTo + " " + distanceFrompt/numberFrom);
		System.out.println(numberTo + " " + numberFrom);
		 numberTo = 0;
		 numberFrom = 0;
		 
		for(Person p: scenario.getPopulation().getPersons().values()) {
			car =false;
			shop = false;
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("walk") ) {
						car = true;
						previousDistance = ((Leg) pe).getRoute().getDistance();
						if (shop == true) {
							distanceFromwalk += previousDistance;
							numberFrom++;
						}
					
						
					}
					else car = false;
				}
				else if (pe instanceof Activity) {
						shop = false;
						if (((Activity) pe).getType().equals( "shopgrocery" ) && scenario.getActivityFacilities().getFacilities().containsKey(((Activity) pe).getFacilityId())) {
							if (car == true) {
							distanceTowalk += previousDistance;
							numberTo++;
							}

							shop = true;
						}
						else shop = false;
					
				}
				
			}
		}
		System.out.println(distanceTowalk/numberTo + " " + distanceFromwalk/numberFrom);
		System.out.println(numberTo + " " + numberFrom);
		 numberTo = 0;
		 numberFrom = 0;
		 int count =0;
		for(Person p: scenario.getPopulation().getPersons().values()) {
			car =false;
			shop = false;
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					//if(((Leg) pe).getMode() !="bike" && ((Leg) pe).getMode() !="car" && ((Leg) pe).getMode() !="pt" && ((Leg) pe).getMode() !="walk") count++;
					if (((Leg) pe).getMode().equals("bike") ) {
						car = true;
						previousDistance = ((Leg) pe).getRoute().getDistance();
						if (shop == true) {
							distanceFrombike += previousDistance;
							numberFrom++;
						}
					
						
					}
					else car = false;
				}
				else if (pe instanceof Activity) {
						shop = false;
					
						if (((Activity) pe).getType().equals( "shopgrocery" ) && scenario.getActivityFacilities().getFacilities().containsKey(((Activity) pe).getFacilityId())) {
							if (((Activity) pe).getFacilityId().equals(Id.create("10220636", ActivityFacility.class)))
								count++;
							if (car == true) {
							distanceTobike += previousDistance;
							numberTo++;
							}

							shop = true;
						}
						else shop = false;
					
				}
				
			}
		}
		System.out.println(distanceTobike/numberTo + " " + distanceFrombike/numberFrom);
		System.out.println(numberTo + " " + numberFrom + " " + count);
		
		 numberTo = 0;
		 numberFrom = 0;
		 distanceFrompt = 0;
		 distanceTopt = 0;
		for(Person p: scenario.getPopulation().getPersons().values()) {
			car =false;
			shop = false;
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					if(!((Leg) pe).getMode().equals("bike") && !((Leg) pe).getMode().equals("car") && !((Leg) pe).getMode().equals("pt") && !((Leg) pe).getMode().equals("walk")) {
						//System.out.println(((Leg) pe).getMode());
						car = true;
						previousDistance = ((Leg) pe).getRoute().getDistance();
						if (shop == true) {
							distanceFrompt += previousDistance;
							numberFrom++;
						}
					
						
					}
					else car = false;
				}
				else if (pe instanceof Activity) {
						shop = false;
						if (((Activity) pe).getType().equals( "shopgrocery" ) && scenario.getActivityFacilities().getFacilities().containsKey(((Activity) pe).getFacilityId())) {
							if (car == true) {
							distanceTopt += previousDistance;
							numberTo++;
							}

							shop = true;
						}
						else shop = false;
					
				}
				
			}
		}
		System.out.println(distanceTopt/numberTo + " " + distanceFrompt/numberFrom);
		System.out.println(numberTo + " " + numberFrom);

		
		
		
	}
	public void travelTimeToFromShops() {

		double distanceTo = 0.0;
		double distanceFrom = 0.0;
		double previousDistance = 0.0;

		boolean shop = false;
		int numberTo = 0;
		int numberFrom = 0;

		boolean car = false;
		
		for(Person p: scenario.getPopulation().getPersons().values()) {
			car =false;
			shop = false;
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("car")) {
						car = true;
						previousDistance = ((Leg) pe).getTravelTime();
						if (shop == true) {
							distanceFrom += previousDistance;
							numberFrom++;
						}
					
						
					}
					else car = false;
				}
				else if (pe instanceof Activity) {
						shop = false;
						if (((Activity) pe).getType().equals( "shopgrocery" ) && scenario.getActivityFacilities().getFacilities().containsKey(((Activity) pe).getFacilityId())) {
							if (car == true) {
							distanceTo += previousDistance;
							numberTo++;
							}

							shop = true;
						}
						else shop = false;
					
				}
				
			}
		}
		
		System.out.println(distanceTo/numberTo + " " + distanceFrom/numberFrom);
		System.out.println(numberTo + " " + numberFrom);
		
		
		
		
		
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		UtilsRetailers ur = new UtilsRetailers(args[0], args[1], args[2]);
		
		ur.distacesToFromShops();
		
		
	}

}
