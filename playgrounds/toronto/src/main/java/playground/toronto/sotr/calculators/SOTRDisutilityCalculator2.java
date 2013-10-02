package playground.toronto.sotr.calculators;

import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import playground.toronto.sotr.routernetwork2.RoutingLink;

public interface SOTRDisutilityCalculator2 {
	
	public double getLinkTravelDisutility(RoutingLink link, double now, Person person, Vehicle vehicle);
	
	public double getTurnTravelDisutility(RoutingLink fromLink, RoutingLink toLink, double now, Person person, Vehicle vehicle);
}
