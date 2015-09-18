package playground.wrashid.msimoni.analyses;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;


public class FindOutAgentsUsingPt implements ActivityStartEventHandler, LinkEnterEventHandler {

	HashMap<Id,Id> usingPt=new HashMap<Id, Id>();
	
	public static void main(String[] args) {

		FindOutAgentsUsingPt findOutPt=new FindOutAgentsUsingPt();
		
		EventsManager events = EventsUtils.createEventsManager();  //create new object of events-manager class
		
		events.addHandler(findOutPt);
		
		//EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		
	
//		String eventsFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/output/equil/ITERS/it.5/5.events.txt.gz";
		String eventsFile="H:/thesis/output_no_pricing_v3_subtours_bugfix/ITERS/it.50/50.events.xml.gz";
		//reader.readFile(eventsFile); //where we find events data
		reader.parse(eventsFile);
		
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
		if (usingPt.containsKey(event.getPersonId())){
			System.out.println(event.getPersonId());
		}
		
	}

}
