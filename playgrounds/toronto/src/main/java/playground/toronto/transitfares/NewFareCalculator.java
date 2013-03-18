package playground.toronto.transitfares;

import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.vehicles.Vehicle;

public interface NewFareCalculator {
	
	public double getDisutilityOfFare(Person person, Vehicle vehicle, TransitRouterNetworkLink link, double now);
	
	public double getFare(Person person, Vehicle vehicle, TransitRouterNetworkLink link, double now);
	
}
