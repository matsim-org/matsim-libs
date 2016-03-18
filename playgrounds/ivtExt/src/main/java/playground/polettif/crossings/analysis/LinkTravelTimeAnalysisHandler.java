package playground.polettif.crossings.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;

import java.util.*;


public class LinkTravelTimeAnalysisHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {
	
	private static final Logger log = Logger.getLogger(LinkTravelTimeAnalysisHandler.class);
	
	List<Id<Link>> linkIds;
	Map<List<Object>, Double> enterEvents = new HashMap<>();
	
	public static Map<Id<Link>, double[]> travelTimes = new TreeMap<>();
	public static Map<Id<Link>, int[]> volumes = new TreeMap<>();
	int startTime;
	int endTime;
	int timeSpan = 24*3600;

	public void setLinkIds(List<Id<Link>> linkIds) {
		this.linkIds = linkIds;
		
		for(Id<Link> entry : linkIds) {
			volumes.put(entry, new int[timeSpan]);
			travelTimes.put(entry, new double[timeSpan]);
		}
	}

	public void setTimeSpan(int startTime, int endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.timeSpan = startTime-endTime;
	}
	
	public void reset(int iteration) {
		System.out.println("reset...");
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {	
		if(linkIds.contains(event.getLinkId())) {
			List<Object> key = new ArrayList<>();
			key.add(event.getVehicleId());
			key.add(event.getLinkId());
			enterEvents.put(key, event.getTime());

		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(linkIds.contains(event.getLinkId())) {
			
			// get corresponding enterEvent
			List<Object> key = new ArrayList<>();
			key.add(event.getVehicleId());
			key.add(event.getLinkId());
			double enterTime = enterEvents.get(key);

			if(enterTime > startTime && enterTime < endTime) {
				double leaveTime = event.getTime();
				double travelTime = leaveTime - enterTime;

				double[] tmpTT = travelTimes.get(event.getLinkId());
				tmpTT[(int) enterTime - startTime] = travelTime;
				travelTimes.put(event.getLinkId(), tmpTT);
			}

			enterEvents.remove(key);
		}
	}

	public Map<Id<Link>, double[]> getTravelTimes() {
		return travelTimes;
	}
	
	public Map<Id<Link>, int[]> getVolumes() {
		return volumes;
	}

}
