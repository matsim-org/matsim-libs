package playground.wrashid.PSF.DataForPSL;

import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterTXT;

public class FilterOutOnlyActStartAndActEndEvents implements ActivityStartEventHandler, ActivityEndEventHandler {

	public static EventsManagerImpl eventManagerForWritingOutEvents;
	
	public static void main(String[] args) {
		EventWriterTXT eventWriterTXT=new EventWriterTXT("a:/temp/filtered-events.txt");
		eventManagerForWritingOutEvents = new EventsManagerImpl();
		eventManagerForWritingOutEvents.addHandler(eventWriterTXT);
		
		EventsManagerImpl eventsManagerImpl = new EventsManagerImpl();
		
		eventsManagerImpl.addHandler(new FilterOutOnlyActStartAndActEndEvents());
		String eventsFilename="A:/data/matsim/output/runRW1003/ITERS/it.0/0.events.txt.gz";
		new MatsimEventsReader(eventsManagerImpl).readFile(eventsFilename);
		
		eventWriterTXT.closeFile();
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	private boolean shouldEventBeWrittenOut(ActivityEvent event){
		return !(event.getActType().equalsIgnoreCase("parkingDeparture") || event.getActType().equalsIgnoreCase("parkingArrival"));
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (shouldEventBeWrittenOut(event)){
			eventManagerForWritingOutEvents.processEvent(event);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (shouldEventBeWrittenOut(event)){
			eventManagerForWritingOutEvents.processEvent(event);
		}
	}
	
}
