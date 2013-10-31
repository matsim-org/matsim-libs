package playground.toronto.sotr.calculators;

import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import playground.toronto.sotr.routernetwork2.AbstractRoutingLink;

public interface SOTRTimeCalculator {
	
	public double getTurnTravelTime(AbstractRoutingLink fromLink, AbstractRoutingLink toLink, double now, Person person, Vehicle vehicle);
	
	public double getLinkTravelTime(AbstractRoutingLink link, double now, Person person, Vehicle vehicle);
}
