package playground.mzilske.events;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;

public class ReadWrite {

	public static void main(String[] args) {
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new EventWriterXML("/Users/michaelzilske/wurst.xml"));
		new MatsimEventsReader(events).readFile("/Users/michaelzilske/d4d/output/0602-capital-only-025freespeed-beginning-disutility-travel-qs/ITERS/it.0/0.events.xml.gz");
	}
	
}
