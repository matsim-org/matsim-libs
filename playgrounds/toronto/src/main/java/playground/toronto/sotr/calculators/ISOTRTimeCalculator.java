package playground.toronto.sotr.calculators;

import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import playground.toronto.sotr.routernetwork.SOTRLink;

public interface ISOTRTimeCalculator {

	public double getTurnTime(SOTRLink fromLink, SOTRLink toLink, double now, Person person, Vehicle vehicle);
	
	public double getLinkTravelTime(SOTRLink link, double time, Person person, Vehicle vehicle);
}
