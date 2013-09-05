package playground.mzilske.events;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.SimStepParallelEventsManagerImpl;

public class SimstepParallel {

	public static void main(String[] args) {
		EventsManager events = new SimStepParallelEventsManagerImpl();
		
	}
	
}
