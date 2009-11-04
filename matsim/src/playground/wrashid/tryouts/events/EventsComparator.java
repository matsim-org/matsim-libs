package playground.wrashid.tryouts.events;

import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;

public class EventsComparator {

	public static void main(String[] args) {
		String eventsFilePath = "c:\\data\\matsim\\input\\runRW1000\\0.events_jdeq.txt";
		
		EventsManagerImpl events1 = new EventsManagerImpl();
		
		EventStatistics eventStatistics=new EventStatistics();
		events1.addHandler(eventStatistics);
		
		events1.initProcessing();
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events1);
		reader.readFile(eventsFilePath);
		events1.finishProcessing();
		
		// read and process second file
		eventsFilePath = "c:\\data\\matsim\\input\\runRW1000\\0.events_pjdeq.txt";
		
		EventsManagerImpl events2 = new EventsManagerImpl();
		
		EventStatistics eventStatistics2=new EventStatistics();
		events2.addHandler(eventStatistics2);
		
		events2.initProcessing();
		reader = new EventsReaderTXTv1(events2);
		reader.readFile(eventsFilePath);
		events2.finishProcessing();
		
		
		
		
		//eventStatistics.printTotalTraveTimes();
		
		System.out.println("===================");
		
		//System.out.println(EventStatistics.compareAgentTripDuration(new IdImpl("104581"), eventStatistics, eventStatistics2));
		
		// TODO: This does not work yet...
		EventStatistics.printTotalTraveTimesAll(eventStatistics, eventStatistics2);
	}
	
}
