package playground.balac.utils.roadpricing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;

public class EventsAnalysis {

	public static void main(String[] args) throws IOException {

		Set<String> links = new TreeSet<String>();
		
		BufferedReader reader = IOUtils.getBufferedReader("C:/Users/balacm/Downloads/TRB/RoadPricingLinks_STRC.txt");
		BufferedWriter writer = IOUtils.getBufferedWriter("C:/Users/balacm/Downloads/TRB/out_notoll.txt");
		String s = reader.readLine();
		
		while (s != null) {
			
			links.add(s);
			s = reader.readLine();
		}
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
			
		LinkEnterHandler linkEnterHandler = new LinkEnterHandler(links);
		
		eventsManager.addHandler(linkEnterHandler);
		
		EventsReaderXMLv1 readerEvents = new EventsReaderXMLv1(eventsManager);
		readerEvents.readFile("C:/Users/balacm/Downloads/TRB/events_notoll.xml.gz");
		
		Map<String, int[]> map = linkEnterHandler.getMapa();
		
		for (String st : map.keySet()) {
			
			writer.write(st);
			
			for (int i : map.get(st))
				writer.write("," + i );
			
			writer.newLine();
		}
		
		writer.flush();
		writer.close();
		
		
	}

}
