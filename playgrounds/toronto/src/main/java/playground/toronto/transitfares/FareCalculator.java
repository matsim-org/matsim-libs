package playground.toronto.transitfares;

import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.vehicles.Vehicle;

/**
 * A calculator which returns information about transit fares.
 * 
 * @author pkucirek
 */
public interface FareCalculator {

	/**
	 * Calculates the actual link fare.
	 * 
	 * @param person The agent being looked at.
	 * @param vehicle The vehicle the agent is traveling in.
	 * @param link The TransitRouterNetworkLink being considered.
	 * @param now The current Matsim time (sec past midnight).
	 * @return The actual fare (in user-defined currency).
	 */
	public double getLinkFare(Person person, Vehicle vehicle, TransitRouterNetworkLink link, double now);	
	
	/**
	 * Calculates the disutility of fares on the link.
	 * 
	 * @param person The agent being looked at.
	 * @param vehicle The vehicle the agent is traveling in.
	 * @param link The TransitRouterNetworkLink being considered.
	 * @param now The current Matsim time (sec past midnight).
	 * @return The generalized disutility of fare on the link.
	 */
	public double getLinkFareDisutility(Person person, Vehicle vehicle, TransitRouterNetworkLink link, double now);
	
}
