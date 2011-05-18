package playground.sergioo.ezLinkDataSimulation;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.utils.io.MatsimXmlParser;

public class SimulationParser implements TransitDriverStartsEventHandler, LinkEnterEventHandler {
	
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new SimulationParser());
		MatsimXmlParser matsimXmlParser = new EventsReaderXMLv1(events);
		matsimXmlParser.parse("./data/ezLinkDataSimulation/output/ITERS/it.0/0.events.xml.gz");
	}

	private PrintWriter printWriter;
	
	/**
	 * @throws FileNotFoundException 
	 * 
	 */
	public SimulationParser() throws FileNotFoundException {
		super();
	}

	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if(event.getTransitRouteId().equals(new IdImpl("243_weekday_1"))) {
			try {
				printWriter = new PrintWriter(new FileWriter("./data/depsTarde.txt",true));
				printWriter.println(event.getTime()+" "+event.getTransitRouteId()+" "+event.getVehicleId()+" "+event.getDepartureId());
				printWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getTime()>86400 && event.getLinkId().equals(new IdImpl("22003_0")))
			try {
				printWriter = new PrintWriter(new FileWriter("./data/depsTarde.txt",true));
				printWriter.println(event.getTime()+" "+event.getPersonId());
				printWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}


	@Override
	public void reset(int iteration) {

	}

}
