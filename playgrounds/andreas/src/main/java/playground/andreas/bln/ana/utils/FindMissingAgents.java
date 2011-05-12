package playground.andreas.bln.ana.utils;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;

public class FindMissingAgents implements EventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler {
	
	private final static Logger log = Logger.getLogger(FindMissingAgents.class);
	
	HashMap<String, Event> agentsOnTour = new HashMap<String, Event>();
	int numberOfStuckedAgents = 0;
	HashMap<String, Integer> linkIDCount = new HashMap<String, Integer>();

	private void readEvents(String filename){
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(this);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(filename);
	}
	
	private void run(String eventsFile) {
		readEvents(eventsFile);
		printStats();
		
	}

	private void printStats() {
		for (Entry<String, Event> agentEntry : this.agentsOnTour.entrySet()) {
			log.info(agentEntry.getKey() + ": " + agentEntry.getValue());
		}
		
		int numberOfPTStucked = 0;
		for (Entry<String, Integer> linkEntry : this.linkIDCount.entrySet()) {
			log.info(linkEntry.getKey() + ": " + linkEntry.getValue().intValue());
			numberOfPTStucked += linkEntry.getValue().intValue();
		}
		log.info(numberOfPTStucked + " PT users stucked on " + this.linkIDCount.size() + " links");
		log.info(this.agentsOnTour.size() + " agents do not arrive");
		log.info(this.numberOfStuckedAgents + " additional agents were stucked");
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		if(this.agentsOnTour.get(event.getPersonId().toString())!= null){
			this.agentsOnTour.remove(event.getPersonId().toString());	
			this.numberOfStuckedAgents++;
			if(event.getLegMode().toString().equalsIgnoreCase(TransportMode.pt.toString())){
				if(this.linkIDCount.get(event.getLinkId().toString()) == null){
					this.linkIDCount.put(event.getLinkId().toString(), new Integer(1));
				} else {
					this.linkIDCount.put(event.getLinkId().toString(), new Integer(this.linkIDCount.get(event.getLinkId().toString()).intValue() + 1));
				}				
			}
		} else {
			log.warn("agent " + event.getPersonId().toString() + " stucked, but never departed");
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
//		if(!event.getPersonId().toString().contains("pt_veh_")){
			if(this.agentsOnTour.get(event.getPersonId().toString()) == null){
				this.agentsOnTour.put(event.getPersonId().toString(), event);
			} else {
				log.warn("agent " + event.getPersonId().toString() + " already on tour");
			}
//		}
		
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if(this.agentsOnTour.get(event.getPersonId().toString()) == null){
			log.warn("agent " + event.getPersonId().toString() + " arrives, but wasn't on tour");
		} else {
			this.agentsOnTour.remove(event.getPersonId().toString());
		}
	}

	public static void main(String[] args) {
		
		new FindMissingAgents().run("F:/counts/770.70.events.xml");
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
}
