package playground.balac.utils;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;


public class TravelTimeOnLinks {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile("C:/Users/balacm/Desktop/network.xml");
        String eventsFile = "C:/Users/balacm/Desktop/FreeSpeedFactor1.110.events.xml.gz";
        EventsManager events = (EventsManager) EventsUtils.createEventsManager();
EventHandler eventHandler = new EventHandler(scenario);
        
        events.addHandler(eventHandler);
        EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
        reader.parse(eventsFile);
		
        
       System.out.println(eventHandler.getTravelTime1() + " " + eventHandler.getTravelTime2() + " " +eventHandler.getTravelTime3());
		
	}
	private static class EventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {
	
		private HashMap<Id, LinkEnterEvent> map;
		private Scenario scenario;
		double speed1 = 0.0;
		double speed2 = 0.0;
		double speed3 = 0.0;
		int count1 = 0;
		int count2 = 0;
		
		int count3 = 0;
	EventHandler(Scenario scenario) {
		 map = new HashMap<Id, LinkEnterEvent>();
		 this.scenario = scenario;
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO Auto-generated method stub
		map.put(event.getDriverId(), event);
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// TODO Auto-generated method stub
		double freespeed = scenario.getNetwork().getLinks().get(event.getLinkId()).getFreespeed();
		double length = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
		if(map.get(event.getDriverId())!= null) {
			double travelTime = event.getTime() - map.get(event.getDriverId()).getTime();
			double effectiveSpeed = length/travelTime;
			if (freespeed < 13.88) {
				speed1 += effectiveSpeed;
				count1++;
			}
			else if (freespeed < 25) {
				speed2 += effectiveSpeed;
				count2++;
			}
			else {
				speed3 += effectiveSpeed;
				count3++;
			}
		}
	}
	
	public double getTravelTime1() {
		return speed1/(double)count1;
	}
	public double getTravelTime2() {
		return speed2/(double)count2;
	}
	public double getTravelTime3() {
		return speed3/(double)count3;
	}
	}
}
