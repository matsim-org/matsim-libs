package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser / Simunto GmbH
 */
public class DefaultRaptorInVehicleCostCalculator implements RaptorInVehicleCostCalculator {

	@Override
	public double getInVehicleCost(double inVehicleTime, double marginalUtility_utl_s, Person person, Vehicle vehicle, RaptorParameters parameters, RouteSegmentIterator iterator) {
		return inVehicleTime * -marginalUtility_utl_s;
	}

}
