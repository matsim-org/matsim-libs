package playground.balac.test;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;


public class Analysis {

	public static void main(String[] args) throws IOException {

		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:\\Users\\balacm\\Desktop\\shopCNRun10.txt");

		
		String inputFile = "C:\\Users\\balacm\\Desktop\\output_events_cn10.xml.gz";

		//create an event object
		EventsManager events = EventsUtils.createEventsManager();

		//create the handler and add it
		EventHandler handler1 = new EventHandler();
		events.addHandler(handler1);
		
        //create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		for (double d: handler1.getWorkEnd()) {
			System.out.println(d);

		}
		System.out.println("=========================================");

		
		for (double d: handler1.getShopDuration()) {
			outLink.write(Double.toString(d));
			outLink.newLine();
		}
		
		outLink.flush();
		outLink.close();
				
		System.out.println("Events file read!");
		
		
	}

}
