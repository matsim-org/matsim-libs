package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;

/**reads an events file and filters only events of the given agents*/  
	class AgentEvents implements BasicEventHandler{
	private final static Logger log = Logger.getLogger(AgentEvents.class);
	private List<String> strIdList;
	Map <String, List<Event>> person2EventsMap = new TreeMap <String, List<Event>>();
	
	public AgentEvents(final List<String> strIdList) {
		this.strIdList = strIdList;
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(Event event) {
		String strPerson = event.getAttributes().get("person");
		if(strPerson!=null && this.strIdList.contains(strPerson)){

			if (!person2EventsMap.keySet().contains(strPerson)){
				person2EventsMap.put(strPerson, new ArrayList<Event>());
			}
			person2EventsMap.get(strPerson).add(event);
		}
		
	}
	
	public static void main(String[] args) {
		String inputEventFile = "../../input/juli/newDemandRerouted/it.1000/1000.events.xml.gz";
		String outputEventFile = "../../input/juli/filteredEvents.xml";
		String[] strIdArray = {"b1060572_2" /*,"b1003990"*/};	
		
		//read and filter out events
		List<String> strIdList = Arrays.asList(strIdArray);
		AgentEvents agEvents = new AgentEvents(strIdList); 
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		events.addHandler((EventHandler) agEvents);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputEventFile);
		log.info("Events file read");

		//write filtered Events
		EventWriterXML eventWriter = new EventWriterXML(outputEventFile);
		for ( List<Event> eventList : agEvents.person2EventsMap.values()){
			for(Event event : eventList){
				eventWriter.handleEvent(event);
			}	
		}
		eventWriter.closeFile();
		log.info("Filtered events file written");
	}
}
