package playground.toronto.transitnetworkutils;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * A simple travel Time/Cost function (assumed to be the same for my purposes!)
 * 
 * @author pkucirek
 *
 */
public class SimpleTravelTimeCalculator implements TravelDisutility, TravelTime {

	public SimpleTravelTimeCalculator(){
	
	}
	
	@Override
	public double getLinkTravelDisutility(Link link, double time,
			Person person, Vehicle vehicle) {
		// TODO Auto-generated method stub
		return link.getLength() / link.getFreespeed();
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		// TODO Auto-generated method stub
		return link.getLength() / link.getFreespeed();
	}

	@Override
	public double getLinkTravelTime(Link link, double time) {
		// TODO Auto-generated method stub
		return link.getLength() / link.getFreespeed();
	}
}
