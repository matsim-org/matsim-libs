package playground.sergioo.ptsim2013;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;

public class EventsToStopStopTimes implements VehicleArrivesAtFacilityEventHandler, PersonLeavesVehicleEventHandler {

	private BasicEventHandler writer;

	public EventsToStopStopTimes(BasicEventHandler writer) {
		this.writer = writer;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		writer.handleEvent(event);
	}
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		writer.handleEvent(event);
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, args[0]);
		EventsManager eventsManager = EventsUtils.createEventsManager(config);
		EventWriterXML writer = new EventWriterXML(args[2]);
		eventsManager.addHandler(new EventsToStopStopTimes(writer));
		(new MatsimEventsReader(eventsManager)).readFile(args[1]);
		writer.closeFile();
	}

}
