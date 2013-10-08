package playground.toronto.analysis.handlers;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class SimplePopulationHandler implements TransitDriverStartsEventHandler, ActivityEndEventHandler {

	private HashSet<Id> pop;
	private HashSet<Id> transitDrivers;
	
	public static void main(String[] args){
		
		String eventsFile = args[0];
		
		SimplePopulationHandler sph = new SimplePopulationHandler();
		
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(sph);
	
		MatsimEventsReader reader = new MatsimEventsReader(em);
		reader.readFile(eventsFile);
		
		System.out.println(sph.getPop().size() + " persons simulated.");
		
	}
	
	public SimplePopulationHandler(){
		this.pop = new HashSet<Id>();
		this.transitDrivers = new HashSet<Id>();
	}

	public HashSet<Id> getPop(){
		return this.pop;
	}
	
	@Override
	public void reset(int iteration) {
		this.pop = new HashSet<Id>();
		this.transitDrivers = new HashSet<Id>();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitDrivers.add(event.getDriverId());		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (this.transitDrivers.contains(event.getPersonId())) return;
		this.pop.add(event.getPersonId());
		
	}
}