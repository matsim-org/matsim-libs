package playground.wrashid.nan;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;


public class FindOutAgentsUsingPt implements ActivityStartEventHandler, LinkEnterEventHandler {

	HashMap<Id,Id> usingPt=new HashMap<Id, Id>();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		FindOutAgentsUsingPt findOutPt=new FindOutAgentsUsingPt();
		
		EventsManager events = EventsUtils.createEventsManager();  //create new object of events-manager class
		
		events.addHandler(findOutPt);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
	
//		String eventsFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/output/equil/ITERS/it.5/5.events.txt.gz";
		String eventsFile="H:/data/experiments/Mohit/10pct ZH/ITERS/it.50/50.events.txt.gz";
		reader.readFile(eventsFile); //where we find events data
		

	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equalsIgnoreCase("pt interaction")){
			usingPt.put(event.getPersonId(),null);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (usingPt.containsKey(event.getDriverId())){
			System.out.println(event.getDriverId());
		}
		
	}

}
