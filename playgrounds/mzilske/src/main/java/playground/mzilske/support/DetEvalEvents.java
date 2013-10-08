package playground.mzilske.support;

import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class DetEvalEvents implements PersonDepartureEventHandler {
	
	public static void main(String[] args) {
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(new DetEvalEvents());
		new MatsimEventsReader(em).readFile("../../run980/ITERS/it.1000/980.1000.events.txt.gz");
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		System.out.println(event.getLegMode());
	}

}
