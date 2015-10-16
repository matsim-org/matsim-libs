package playground.balac.twowaycarsharing.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;


public class DistanceTimeCS {

	EventsManager events = (EventsManager) EventsUtils.createEventsManager();
    EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
	
	
	public void run(String[] args){
		final BufferedWriter outLink = IOUtils.getBufferedWriter(args[2]);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
		
		RentalTimes rentalTimes = new RentalTimes(scenario, outLink);		
		events.addHandler(rentalTimes);
    	reader.parse(args[1]);
		
    	
		
	}
	private static class RentalTimes implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler {

		HashMap<Id, Double> startTimes = new HashMap<Id, Double>();
		HashMap<Id, Double> distance = new HashMap<Id, Double>();
		ScenarioImpl scenario;
		
		final BufferedWriter outLink;
		
		double d = 0.0;
		int i = 0;
		RentalTimes(ScenarioImpl scenario, BufferedWriter outLink) {
			this.scenario = scenario;
			this.outLink = outLink;
		}
		
		
		


		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}


		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			// TODO Auto-generated method stub
			
			if (event.getVehicleId().toString().contains("W"))
				startTimes.put(event.getPersonId(), event.getTime());
				distance.put(event.getPersonId(), 0.0);
			
			
		}


		@Override
		public void handleEvent(PersonLeavesVehicleEvent event) {
			// TODO Auto-generated method stub
			
			if (event.getVehicleId().toString().contains("W")) {
				
				try {
					//if (i % 2 == 1){
					outLink.write(event.getPersonId().toString());
				
					outLink.write(" ");
					
					outLink.write(event.getVehicleId().toString());
					
					outLink.write(" ");
					
					outLink.write(startTimes.get(event.getPersonId()).toString());
					
					outLink.write(" ");
					
					outLink.write(Double.toString(event.getTime()));
					
					outLink.write(" ");
					
					outLink.write(Double.toString(distance.get(event.getPersonId())));
					
					outLink.newLine();
					outLink.flush();
					
					
					startTimes.remove(event.getPersonId());
					distance.remove(event.getPersonId());
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				i++;
			}
			
		}


		@Override
		public void handleEvent(LinkEnterEvent event) {
			// TODO Auto-generated method stub
			
			if (startTimes.containsKey(event.getDriverId())) {
				
				double d = distance.get(event.getDriverId());
				
				d += scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
				distance.put(event.getDriverId(), d);
			}
			
		}
		
		@Override
		public void handleEvent(PersonDepartureEvent event) {
			// TODO Auto-generated method stub
			
			if (event.getPersonId().toString().equals("1879432")) { 
				System.out.println(event.getTime());
				System.out.println(event.getLegMode());
			}
			
			
		}
		
		
		
		
		
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DistanceTimeCS f = new DistanceTimeCS();
		f.run(args);		

	}

}
