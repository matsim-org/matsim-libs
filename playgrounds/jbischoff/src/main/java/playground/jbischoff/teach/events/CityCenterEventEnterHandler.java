package playground.jbischoff.teach.events;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class CityCenterEventEnterHandler implements LinkEnterEventHandler {

	
	List<Id<Person>> agentsInCityCenter = new ArrayList<>();
	List<Id<Link>> cityCenterLinks = new ArrayList<>();
	
	@Override
	public void reset(int iteration) {
		this.agentsInCityCenter.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.cityCenterLinks.contains(event.getLinkId()))
		{
		this.agentsInCityCenter.add(event.getPersonId());
		}
	}
	public void addLinkId(Id<Link> linkId){
		this.cityCenterLinks.add(linkId);
	}

	public List<Id<Person>> getAgentsInCityCenter() {
		return agentsInCityCenter;
	}

	
}