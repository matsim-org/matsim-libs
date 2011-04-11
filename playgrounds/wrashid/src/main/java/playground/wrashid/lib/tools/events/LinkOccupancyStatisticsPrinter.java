package playground.wrashid.lib.tools.events;

import java.util.HashMap;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterTXT;

import playground.wrashid.PSF.DataForPSL.FilterOutOnlyActStartAndActEndEvents;
import playground.wrashid.lib.obj.IntegerValueHashMap;

public class LinkOccupancyStatisticsPrinter implements LinkEnterEventHandler {

	IntegerValueHashMap<String> linkToFrequencyCounts;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EventsManager eventsManagerImpl = (EventsManager) EventsUtils.createEventsManager();
		
		LinkOccupancyStatisticsPrinter statistics=new LinkOccupancyStatisticsPrinter();
		
		eventsManagerImpl.addHandler(statistics);
		String eventsFilename="C:/data/My Dropbox/Temp/run27/150.events.txt.gz";
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
