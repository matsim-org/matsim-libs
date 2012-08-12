package playground.toronto.analysis.handlers;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.PersonEventHandler;

public class SimplePopulationHandler implements PersonEventHandler {

	private HashSet<Id> pop;
	
	public SimplePopulationHandler(){
		this.pop = new HashSet<Id>();
	}

	public HashSet<Id> getPop(){
		return this.pop;
	}
	
	@Override
	public void reset(int iteration) {
		this.pop = new HashSet<Id>();
	}

	@Override
	public void handleEvent(PersonEvent event) {
		this.pop.add(event.getPersonId());
	}
}
