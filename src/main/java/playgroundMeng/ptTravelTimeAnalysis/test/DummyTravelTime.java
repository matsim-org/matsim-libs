package playgroundMeng.ptTravelTimeAnalysis.test;

import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class DummyTravelTime implements TravelTime{
	Map<Link, Map<Double, Double>> link2Time2TravelTime = new HashedMap();

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		// TODO Auto-generated method stub
		return link2Time2TravelTime.get(link).get(time);
	}

}
