package playground.wrashid.PSF.DataForPSL;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterTXT;

public class FilterOutOnlyActStartAndActEndEvents implements ActivityStartEventHandler, ActivityEndEventHandler {

	public static EventsManager eventManagerForWritingOutEvents;
	
	public static void main(String[] args) {
		EventWriterTXT eventWriterTXT=new EventWriterTXT("a:/temp/filtered-events.txt");
		eventManagerForWritingOutEvents = (EventsManager) EventsUtils.createEventsManager();
		eventManagerForWritingOutEvents.addHandler(eventWriterTXT);
		
		EventsManager eventsManagerImpl = (EventsManager) EventsUtils.createEventsManager();
		
		eventsManagerImpl.addHandler(new FilterOutOnlyActStartAndActEndEvents());
		String eventsFilename="A:/data/matsim/output/runRW1003/ITERS/it.0/0.events.txt.gz";
		new MatsimEventsReader(eventsManagerImpl).readFile(eventsFilename);
		
		eventWriterTXT.closeFile();
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	private boolean shouldEventBeWrittenOut(String actType){
		return !(actType.equalsIgnoreCase("parkingDeparture") || actType.equalsIgnoreCase("parkingArrival"));
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (shouldEventBeWrittenOut(event.getActType())){
			eventManagerForWritingOutEvents.processEvent(event);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (shouldEventBeWrittenOut(event.getActType())){
			eventManagerForWritingOutEvents.processEvent(event);
		}
	}
	
}
