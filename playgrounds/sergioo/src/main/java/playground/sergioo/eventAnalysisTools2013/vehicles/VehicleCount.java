package playground.sergioo.eventAnalysisTools2013.vehicles;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class VehicleCount implements LinkEnterEventHandler {

	private static Map<Id, Integer> numCars = new HashMap<Id, Integer>();
	private static Map<Id, Integer> numBuses = new HashMap<Id, Integer>();
	private static Map<Id, Integer> numTrucks = new HashMap<Id, Integer>();

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getPersonId().toString().startsWith("pt")) {
			Integer numBus = numBuses.get(event.getLinkId());
			if(numBus==null)
				numBus = 0;
			numBuses.put(event.getLinkId(), numBus+1);
		}
		else if(event.getPersonId().toString().startsWith("stTG")) {
			Integer numTruck = numTrucks.get(event.getLinkId());
			if(numTruck==null)
				numTruck = 0;
			numTrucks.put(event.getLinkId(), numTruck+1);
		}
		else {
			Integer numCar = numCars.get(event.getLinkId());
			if(numCar==null)
				numCar = 0;
			numCars.put(event.getLinkId(), numCar+1);
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new VehicleCount());
		new MatsimEventsReader(events).readFile(args[1]);
		PrintWriter writer = new PrintWriter(args[2]);
		writer.println("link id,number of cars,number of buses,number of trucks");
		for(Link link:scenario.getNetwork().getLinks().values()) {
			Integer numCar = numCars.get(link.getId());
			Integer numBus = numBuses.get(link.getId());
			Integer numTruck = numTrucks.get(link.getId());
			writer.println(link.getId()+","+numCar==null?0:numCar+","+numBus==null?0:numBus+","+numTruck==null?0:numTruck);
		}
		writer.close();
	}

}
