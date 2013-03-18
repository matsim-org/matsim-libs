package playground.toronto.transitfares.deprecated;

import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.vehicles.Vehicle;



/**
 * Always returns 0 disutility of fares.
 * 
 * @author pkucirek
 *
 */
public class NullFareCalculator implements FareCalculator {

	@Override
	public double getDisutilityOfTransferFare(Person person, Vehicle vehicle,
			TransitRouterNetworkLink link, double now) {
		return 0;
	}

	@Override
	public double getDisutilityOfInVehicleFare(Person person, Vehicle vehicle,
			TransitRouterNetworkLink link, double now) {
		return 0;
	}



}
