package org.matsim.codeexamples.converter.eventsTxtToXml;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderTXT;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

import java.io.IOException;

/**
 * Read in the (rather old) matsim txt events format, and write out in current xml format.
 * <br/>
 * People have asked why we did not stay with the tab-separated txt events since they would be so much easier to
 * process.  The answer is that in our newer events, too many of the fields depend on the event type.  Meaning
 * that they could not be characterized by a header, and therefore need some in-record typization as provided
 * e.g. by xml, or in newer times in json.
 */
class RunEventsTXT2XMLExample {
	
	public static void main( String [] args ) throws IOException {
		
		EventsManager events = EventsUtils.createEventsManager();
		
		final EventWriterXML writer = new EventWriterXML("output_events.xml.gz");
		events.addHandler(writer);
		
		new EventsReaderTXT(events).runEventsFile("input_events.txt.gz");
		
		writer.closeFile();
		
	}
	
}
