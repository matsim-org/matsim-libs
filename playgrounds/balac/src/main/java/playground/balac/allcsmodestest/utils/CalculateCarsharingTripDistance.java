package playground.balac.allcsmodestest.utils;

import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;



public class CalculateCarsharingTripDistance {
	EventsManager events = (EventsManager) EventsUtils.createEventsManager();
    EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
    
	Network network;
	ScenarioImpl scenario;
	
	public CalculateCarsharingTripDistance(String networkFilePath) {
		
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFilePath);
	}
	 public void run(String s) throws IOException{
	    	
			Purpose purpose = new Purpose(scenario.getNetwork());
	    	
	    	events.addHandler(purpose);
	    	reader.parse(s);
	    	for (int i = 0; i < purpose.distanceTraveled.length; i++) 
				System.out.println((double)purpose.distanceTraveled[i]/(double)purpose.count * 100.0);
	    }
	 private static class Purpose implements  PersonLeavesVehicleEventHandler, PersonEntersVehicleEventHandler, LinkLeaveEventHandler {
		 int[] distanceTraveled = new int[160];
			int count = 0;
			double distance = 0.0;
			HashMap<Id, Double> personVehicles = new HashMap<Id, Double>();

			Network network;
	public Purpose (Network network) {
		this.network = network;
	}
			
			@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		personVehicles = new HashMap<Id, Double>();
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// TODO Auto-generated method stub
		if (event.getVehicleId().toString().startsWith("TW")) {
			distanceTraveled[(int)(personVehicles.remove(event.getVehicleId()) / 1000)]++;
			//distance += personVehicles.remove(event.getVehicleId());
			count++;
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// TODO Auto-generated method stub
		if (event.getVehicleId().toString().startsWith("TW"))
			personVehicles.put(event.getVehicleId(), 0.0);
	}


	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// TODO Auto-generated method stub
		if (event.getVehicleId().toString().startsWith("TW")) {
			
			double d = personVehicles.get(event.getVehicleId());
			personVehicles.put(event.getVehicleId(), d + network.getLinks().get(event.getLinkId()).getLength());
			
		}
				
	}
	 }
	public static void main(String[] args) throws IOException {
		CalculateCarsharingTripDistance c = new  CalculateCarsharingTripDistance(args[0]);
		c.run(args[1]);
		
	}

}
