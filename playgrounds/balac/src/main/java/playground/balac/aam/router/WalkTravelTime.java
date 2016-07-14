package playground.balac.aam.router;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class WalkTravelTime implements TravelTime {

	HashMap<Id<Link>, Double> linkWalkTravelTimes = new HashMap<Id<Link>, Double>(); 
//	private static final Logger log = Logger.getLogger(WalkTravelTime.class);

	public WalkTravelTime (Scenario scenario, HashMap<Id<Link>, Double> linkSpeeds)  {
		
		for (Link l : scenario.getNetwork().getLinks().values()) {
			if (linkSpeeds.containsKey(l.getId()))
				linkWalkTravelTimes.put(l.getId(), l.getLength() / (10 *linkSpeeds.get(l.getId())));
			else
				linkWalkTravelTimes.put(l.getId(), l.getLength() / Double.parseDouble(scenario.getConfig().getModule("planscalcroute").getParams().get("teleportedModeSpeed_walk")));
			
		}
		
		/*for (Link l : scenario.getNetwork().getLinks().values()) {
			
			linkWalkTravelTimes.put(l.getId(), l.getLength() / 5.0);
			
		}*/
	}
	
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person,
			Vehicle vehicle) {
		// TODO Auto-generated method stub
		return linkWalkTravelTimes.get(link.getId());
	}

}
