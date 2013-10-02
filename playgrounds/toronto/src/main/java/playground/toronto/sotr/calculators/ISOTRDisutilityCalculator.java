package playground.toronto.sotr.calculators;

import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import playground.toronto.sotr.routernetwork.SOTRLink;

public interface ISOTRDisutilityCalculator {

	public double getTurnTravelDisutility(SOTRLink fromLink, SOTRLink toLink, double time, Person person, Vehicle vehicle);
	
	public double getLinkTravelDisutility(SOTRLink link, double time, Person person, Vehicle vehicle);
}
