package playground.toronto.transitfares;

import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.vehicles.Vehicle;


/**
 * Essentially a null calculator; always returns a 0 fare.
 * 
 * @author pkucirek
 *
 */
public class NullFareCalculator implements FareCalculator {

	@Override
	public double getTransferFare(Person person, Vehicle vehicle,
			TransitRouterNetworkLink link, double now) {
		return 0;
	}

	@Override
	public double getInVehicleFare(Person person, Vehicle vehicle,
			TransitRouterNetworkLink link, double now) {
		return 0;
	}



}
