package playground.wrashid.lib.tools.events;

import java.util.HashMap;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterTXT;

import playground.wrashid.PSF.DataForPSL.FilterOutOnlyActStartAndActEndEvents;

public class LinkOccupancyStatisticsPrinter implements LinkEnterEventHandler {

	IntegerValueHashMap<String> linkToFrequencyCounts;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EventsManager eventsManagerImpl = (EventsManager) EventsUtils.createEventsManager();
		
		LinkOccupancyStatisticsPrinter statistics=new LinkOccupancyStatisticsPrinter();
		
		eventsManagerImpl.addHandler(statistics);
		String eventsFilename="";
		new MatsimEventsReader(eventsManagerImpl).readFile(eventsFilename);
		
		statistics.printStatistics();
	}
	
	public LinkOccupancyStatisticsPrinter(){
		this.linkToFrequencyCounts=new IntegerValueHashMap<String>();
	}

	private void printStatistics() {
		System.out.println("linkId"+"\t"+"numberOfVehiclesOnLink");
		
		for (String linkIdString:linkToFrequencyCounts.getKeySet()){
			System.out.println(linkIdString + "\t" + linkToFrequencyCounts.get(linkIdString));
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		linkToFrequencyCounts.increment(event.getLinkId().toString());
	}

	@Override
	public void reset(int iteration) {
		
	}

}
