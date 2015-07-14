package playground.sergioo.cepasToMATSim2013;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class CompleteBusArrivalEvents implements VehicleArrivesAtFacilityEventHandler {

	private Map<Id<Vehicle>, Id<TransitStopFacility>> lastStopsVehicle = new HashMap<Id<Vehicle>, Id<TransitStopFacility>>(); 
	
	public CompleteBusArrivalEvents() {
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id<TransitStopFacility> lastStop = lastStopsVehicle.get(event.getVehicleId());
		if(lastStop != null) {
			
		}
		lastStopsVehicle.put(event.getVehicleId(), event.getFacilityId());
	}
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		EventsManager outEventsManager = EventsUtils.createEventsManager();
		outEventsManager.addHandler(new EventWriterXML(args[2]));
		EventsManager inEventsManager = EventsUtils.createEventsManager();
		inEventsManager.addHandler(new CompleteBusArrivalEvents());
		new EventsReaderXMLv1(inEventsManager).parse(args[1]);
	}

}
