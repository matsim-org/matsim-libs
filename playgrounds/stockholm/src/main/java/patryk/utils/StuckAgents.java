package patryk.utils;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;



public class StuckAgents implements PersonStuckEventHandler {
	ArrayList<Id<Link>> linkIDs = new ArrayList<>();
	ArrayList<Id<Person>> personIDs = new ArrayList<>();

	@Override
	public void reset(int iteration) {
		linkIDs.clear();
		personIDs.clear();
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		linkIDs.add(event.getLinkId());
		personIDs.add(event.getPersonId());
	}
	
	public ArrayList<Id<Link>> getPersonStuck() {
		return linkIDs;
	}
	
	public ArrayList<Id<Person>> getPersonStuckIDs() {
		return personIDs;
	}

}
