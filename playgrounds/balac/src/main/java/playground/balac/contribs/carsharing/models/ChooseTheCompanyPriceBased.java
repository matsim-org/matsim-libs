package playground.balac.contribs.carsharing.models;

import java.util.Map;
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
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.models.ChooseTheCompany;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

public class ChooseTheCompanyPriceBased implements ChooseTheCompany {
	@Inject private MembershipContainer memberships;
	//@Inject private CarsharingSupplyContainer carsharingSupply;
	@Inject private CostsCalculatorContainer costCalculator;
	@Inject private Scenario scenario;
	
	@Inject private LeastCostPathCalculatorFactory pathCalculatorFactory ;
	
	@Inject private Map<String, TravelTime> travelTimes ;
	@Inject private Map<String, TravelDisutilityFactory> travelDisutilityFactories ;
	
	@Override
	public String pickACompany(Plan plan, Leg leg, double now) {

		//=== pick a company based on a logit model 
		//=== that takes the potential price into account
		
		String carsharingType = leg.getMode();
		
		Person person = plan.getPerson();
		Id<Person>  personId = person.getId();
		String mode = leg.getMode();
		Set<String> availableCompanies = this.memberships.getPerPersonMemberships().get(personId).getMembershipsPerCSType().get(mode);
		
		RentalInfo rentalInfo = new RentalInfo();
		double time = estimatetravelTime(leg, person, now);
		rentalInfo.setInVehicleTime(time);
		rentalInfo.setStartTime(now);
		rentalInfo.setEndTime(now + time); 
		String chosenCompany = "";
		double price = 0.0;
		for(String companyName : availableCompanies) {
			//estimate the rental price for each company
			if (price == 0.0 || costCalculator.getCost(companyName, carsharingType, rentalInfo) < price)
				chosenCompany = companyName;
			
		}
		
		return chosenCompany;
	}

	private double estimatetravelTime(Leg leg, Person person, double now) {	
		
		Network network = scenario.getNetwork();
		TravelTime travelTime = travelTimes.get( TransportMode.car ) ;
		
		TravelDisutility travelDisutility = travelDisutilityFactories.get( TransportMode.car ).createTravelDisutility(travelTime) ;
		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(scenario.getNetwork(),
				travelDisutility, travelTime ) ;
		Link startLink = network.getLinks().get(leg.getRoute().getStartLinkId());
		Link endLink  = network.getLinks().get(leg.getRoute().getStartLinkId());
		Vehicle vehicle = null ;
		Path path = pathCalculator.calcLeastCostPath(startLink.getToNode(), endLink.getFromNode(), 
				now, person, vehicle ) ;
		
		return path.travelTime;
		
	}

}
