package playground.toronto.transitfares.deprecated;

import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.vehicles.Vehicle;

/**
 * <p>A calculator which returns the disutility of transit fares.</p>
 * <p>Has two methods: <code>getDisutilityOfTransferFare</code> for transferring to/from transit, and <code>
 * getDisutilityOfInVehicleFare</code> which is accrued in-vehicle.</p>
 * 
 * @author pkucirek
 */
public interface FareCalculator {
	
	/**
	 * Calculates the disutility of fare(s) on access, egress, and transfer links
	 * 
	 * @param person The agent being looked at.
	 * @param vehicle The vehicle the agent is traveling in.
	 * @param link The TransitRouterNetworkLink being considered.
	 * @param now The current Matsim time (sec past midnight).
	 * @return The actual fare (in user-defined currency).
	 */
	public double getDisutilityOfTransferFare(Person person, Vehicle vehicle, TransitRouterNetworkLink link, double now);	
	
	/**
	 *  Calculates the disutility of fare(s) on in-vehicle links
	 * 
	 * @param person The agent being looked at.
	 * @param vehicle The vehicle the agent is traveling in.
	 * @param link The TransitRouterNetworkLink being considered.
	 * @param now The current Matsim time (sec past midnight).
	 * @return The actual fare (in user-defined currency).
	 */
	public double getDisutilityOfInVehicleFare(Person person, Vehicle vehicle, TransitRouterNetworkLink link, double now);
	
}
