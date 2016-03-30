package playground.polettif.crossings.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;

import java.util.*;


public class LinkAnalysisHandler extends AnalysisHandler {
	
	private static final Logger log = Logger.getLogger(LinkAnalysisHandler.class);
	
	List<Id<Link>> linkIds;
	Map<List<Object>, Double> enterEvents = new HashMap<>();
	
	public static Map<Id<Link>, double[]> travelTimes = new TreeMap<>();

	public static Map<Id<Link>, LinkVolumeStat> linksVolumes = new HashMap<>();

	int startTime;
	int endTime;
	int timeSpan = 24*3600;

	public void setTimeSpan(int startTime, int endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.timeSpan = endTime-startTime;
	}


	public void setLinkIds(List<Id<Link>> linkIds) {
		this.linkIds = linkIds;

		for(Id<Link> entry : linkIds) {
			travelTimes.put(entry, new double[timeSpan]);
			linksVolumes.put(entry, new LinkVolumeStat());
		}
	}


	@Override
	public void handleEvent(LinkEnterEvent event) {

		if(linkIds.contains(event.getLinkId())) {
			// TravelTimes
			List<Object> key = new ArrayList<>();
			key.add(event.getVehicleId());
			key.add(event.getLinkId());
			enterEvents.put(key, event.getTime());

			// Link Volumes
			if(event.getTime() > startTime && event.getTime() < endTime) {
				linksVolumes.get(event.getLinkId()).addVehicle((int) event.getTime());
			}
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

			// Link Volumes
			if(event.getTime() > startTime && event.getTime() < endTime) {
				if(linksVolumes.get(event.getLinkId()).getCurrentValue() > 0)
					linksVolumes.get(event.getLinkId()).subtractVehicle((int) event.getTime());
			}
		}
	}

	public Map<Id<Link>, double[]> getVolumes() {
		Map<Id<Link>, double[]> linkVolumesTable = new TreeMap<>();

		for(Map.Entry<Id<Link>, LinkVolumeStat> entry : linksVolumes.entrySet()) {
			double[] tmpVolumes = new double[timeSpan];

			for(Map.Entry<Integer, Double> linkVolumeEntry : entry.getValue().getMap().entrySet()) {
				tmpVolumes[linkVolumeEntry.getKey()-startTime] = linkVolumeEntry.getValue();
			}

			int i = 1;
			while(i < tmpVolumes.length) {
				if(tmpVolumes[i] == 0) {
					tmpVolumes[i] = tmpVolumes[i-1];
				}
				i++;
			}
			linkVolumesTable.put(entry.getKey(), tmpVolumes);
		}

		return linkVolumesTable;
	}

	public Map<Id<Link>,double[]> getTravelTimes() {
		return travelTimes;
	}

	@Override
	public void reset(int iteration) {
		System.out.println("reset...");
	}

	public class LinkVolumeStat {

		Map<Integer, Double> linkVolumes = new TreeMap<>(); // <time, value>
		double currentVolume = 0;

		public LinkVolumeStat() {
		}

		public void addVehicle(int timestamp) {
			linkVolumes.put(timestamp, ++currentVolume);
		}

		public void subtractVehicle(int timestamp) {
			linkVolumes.put(timestamp, --currentVolume);
		}

		public double getCurrentValue() {
			return currentVolume;
		}

		public Map<Integer, Double> getMap() {
			return linkVolumes;
		}
	}

}

