package playground.toronto.ttimematrix;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import playground.toronto.analysis.handlers.AgentTripChainHandler;

/**
 * Gets OD matrices for trip components from an events file.
 * 
 * @author pkucirek
 *
 */
public class AnalyzeTripComponentsFromEvents {

	public static void main(String[] args){
		String eventsFile= "";
		
		AgentTripChainHandler atch = new AgentTripChainHandler(null /* TRANSIT SCHEDULE*/);
		atch.setLinkZoneMap(null /*LINK2ZONE MAP*/);
		
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(atch);
		MatsimEventsReader reader = new MatsimEventsReader(em);
		reader.readFile(eventsFile);
		
		
		
	}
	
}
