package playground.toronto.transitfares;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.vehicles.Vehicle;

public class MultipleAgencyFareCalculator implements NewFareCalculator {

	private Map<Id, Id> lineToAgencyMap;
	private Map<Id, NewFareCalculator> agencyCalculatorMap;
	private Map<Tuple<Id, Id>, NewFareCalculator> transferFareCalculators = null;
	
	
	@Override
	public double getDisutilityOfFare(Person person, Vehicle vehicle,
			TransitRouterNetworkLink link, double now) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getFare(Person person, Vehicle vehicle,
			TransitRouterNetworkLink link, double now) {
		// TODO Auto-generated method stub
		return 0;
	}

}
