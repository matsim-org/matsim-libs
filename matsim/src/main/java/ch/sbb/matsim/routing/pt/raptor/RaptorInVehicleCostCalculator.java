package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser / Simunto GmbH
 */
public interface RaptorInVehicleCostCalculator {

	double getInVehicleCost(double inVehicleTime, double marginalUtility_utl_s, Person person, Vehicle vehicle, RaptorParameters paramters, RouteSegmentIterator iterator);

	interface RouteSegmentIterator {
		boolean hasNext();
		void next();
		double getInVehicleTime();
		double getPassengerCount();
		double getTimeOfDay();
	}

}
