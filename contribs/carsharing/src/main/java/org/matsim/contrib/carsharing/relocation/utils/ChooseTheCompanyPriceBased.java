package org.matsim.contrib.carsharing.relocation.utils;

import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.models.ChooseTheCompany;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

public class ChooseTheCompanyPriceBased implements ChooseTheCompany {
	@Inject private MembershipContainer memberships;
	@Inject private CarsharingSupplyInterface carsharingSupply;
	@Inject private CostsCalculatorContainer costCalculator;
	@Inject private Scenario scenario;
	
	@Inject private LeastCostPathCalculatorFactory pathCalculatorFactory ;
	
	@Inject private Map<String, TravelTime> travelTimes ;
	@Inject private Map<String, TravelDisutilityFactory> travelDisutilityFactories ;
	
	
	@Override
	public String pickACompany(Plan plan, Leg leg, double now, String vehicleType) {

		//=== pick a company based on a logit model 
		//=== that takes the potential price into account
		Network network = scenario.getNetwork();
		String carsharingType = leg.getMode();
		
		Person person = plan.getPerson();
		Id<Person>  personId = person.getId();
		String mode = leg.getMode();
		Set<String> availableCompanies = this.memberships.getPerPersonMemberships().get(personId).getMembershipsPerCSType().get(mode);
		
		
		String chosenCompany = "";
		double price = -1.0;
		Random random = MatsimRandom.getRandom();
		
		Link startLink = network.getLinks().get(leg.getRoute().getStartLinkId());
		double minDistance = -1;
		for(String companyName : availableCompanies) {
			//estimate the rental price for each company
			
			CSVehicle vehicle = this.carsharingSupply.findClosestAvailableVehicle(network.getLinks().get(leg.getRoute().getStartLinkId()),
					mode, vehicleType, companyName, 1000.0);
			
			/*if (vehicle != null) {
				
				Link vehicleLocation = this.carsharingSupply.getCompany(companyName).getVehicleContainer(mode).getVehicleLocation(vehicle);

				double distance = CoordUtils.calcEuclideanDistance(startLink.getCoord(), vehicleLocation.getCoord());
				double x = random.nextBoolean() ? -0.0001 : + 0.0001;

				if (minDistance == -1 || distance + x < minDistance) {
					chosenCompany = companyName;
					minDistance = distance;
				}
				
			}*/
				
			
			
			
			
			
			
			if (vehicle != null) {
				Link vehicleLocation = this.carsharingSupply.getCompany(companyName).getVehicleContainer(mode).getVehicleLocation(vehicle);
				
				double walkTravelTime = estimateWalkTravelTime(startLink, vehicleLocation);
				
				RentalInfo rentalInfo = new RentalInfo();
				double time = estimatetravelTime(leg, vehicleLocation, person, now);
				rentalInfo.setInVehicleTime(time);
				rentalInfo.setStartTime(now);
				rentalInfo.setEndTime(now + walkTravelTime + time); 
				double x = random.nextBoolean() ? -0.0001 : + 0.0001;
				if (price == -1.0 || costCalculator.getCost(companyName, carsharingType, rentalInfo) +
						x < price) {
					
						chosenCompany = companyName;
						price = costCalculator.getCost(companyName, carsharingType, rentalInfo);					
				}			
			}
		}
		
		return chosenCompany;
	}

	private double estimatetravelTime(Leg leg, Link vehicleLocation, Person person, double now) {	
		
		Network network = scenario.getNetwork();
		TravelTime travelTime = travelTimes.get( TransportMode.car ) ;
		
		TravelDisutility travelDisutility = travelDisutilityFactories.get( TransportMode.car ).createTravelDisutility(travelTime) ;
		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(scenario.getNetwork(),
				travelDisutility, travelTime ) ;
		Link endLink  = network.getLinks().get(leg.getRoute().getStartLinkId());
		Vehicle vehicle = null ;
		Path path = pathCalculator.calcLeastCostPath(vehicleLocation.getToNode(), endLink.getFromNode(), 
				now, person, vehicle ) ;
		
		return path.travelTime;
		
	}
	
	private double estimateWalkTravelTime(Link startLink, Link endLink) {	
		
		return  CoordUtils.calcEuclideanDistance(startLink.getCoord(), endLink.getCoord()) * 1.3 / 1.0;
		
	}

}
