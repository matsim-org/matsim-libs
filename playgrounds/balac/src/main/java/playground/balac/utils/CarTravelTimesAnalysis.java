package playground.balac.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;



public class CarTravelTimesAnalysis {
	EventsManager events = (EventsManager) EventsUtils.createEventsManager();
    EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
    public void run(String s, Scenario scenario) throws IOException{
    	
		Purpose purpose = new Purpose(scenario);
    	
    	events.addHandler(purpose);
    	reader.parse(s);
    	System.out.println(purpose.averageSpeed());
    }
    
    private static class Purpose implements   PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler {
		Set<String> fahrzugIDs = new TreeSet<String>();
		
		double travelTime = 0.0;
		double travelDistance = 0.0;
		Scenario scenario;
		Map<String, Double> travelTimes = new HashMap<String, Double>();
		
		Purpose (Scenario scenario) {
			
			this.scenario = scenario;
		}
		
		public double averageSpeed() {
			return this.travelDistance/this.travelTime;
		}
	
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

	
		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			// TODO Auto-generated method stub
			if (!event.getVehicleId().toString().startsWith("TW")) {
				
				travelTimes.put(event.getVehicleId().toString(), event.getTime());
				
			}
		}
		
		@Override
		public void handleEvent(PersonLeavesVehicleEvent event) {
			// TODO Auto-generated method stub
			if (!event.getVehicleId().toString().startsWith("TW")) {
				travelTime += (event.getTime() - travelTimes.remove(event.getVehicleId().toString()));
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			// TODO Auto-generated method stub
			
			travelDistance += scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			
			
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			// TODO Auto-generated method stub
			//travelTimes.put(event.getVehicleId().toString(), event.getTime());
		}
	
		
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
		
		CarTravelTimesAnalysis c = new  CarTravelTimesAnalysis();
		c.run(args[1], scenario);
	}

}
