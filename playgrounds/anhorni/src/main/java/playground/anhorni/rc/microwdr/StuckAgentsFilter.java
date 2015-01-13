package playground.anhorni.rc.microwdr;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

public class StuckAgentsFilter implements AgentFilter {
	
	private static final Logger log = Logger.getLogger(StuckAgentsFilter.class);
	private Network network;
	private final Map<Id<Person>, MobsimAgent> agents;
	private TravelTimeCollector traveltimeCollector;
	
	// use the factory
	/*package*/ StuckAgentsFilter(Map<Id<Person>, MobsimAgent> agents, 
			TravelTimeCollector traveltimeCollector,
			Network network) {
		this.agents = agents;
		this.network = network;	
		this.traveltimeCollector = traveltimeCollector;
	}

	@Override
	public void applyAgentFilter(Set<Id<Person>> set, double time) {
		log.info("this one is not used anymore ...");	
	}

	@Override
	public boolean applyAgentFilter(Id<Person> id, double time) {
		MobsimAgent agent = this.agents.get(id);
		Link currentLink = this.network.getLinks().get(agent.getCurrentLinkId());
		double length = currentLink.getLength();
		double speed = currentLink.getFreespeed();
		double freeflowTraveltime = length / speed;
		
		double currentLinkTravelTime = traveltimeCollector.getLinkTravelTime(currentLink, 0.0, null, null);
						
		if (currentLinkTravelTime > 2.0 * freeflowTraveltime) {
			return true;
		} else {
			return false;
		}
	}
}
