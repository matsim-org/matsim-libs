package playground.polettif.crossings.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.*;


public class LinkAnalysisHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

	private static final Logger log = Logger.getLogger(LinkAnalysisHandler.class);

	List<Id<Link>> linkIds;
	Map<List<Object>, Double> enterEvents = new HashMap<>();

	private Map<Id<Link>, double[]> travelTimes = new TreeMap<>();

	private Map<Id<Link>, LinkVolumeStat> linksVolumes = new HashMap<>();

	private Map<String, Map<Double, Double>> timeSpaceMap = new HashMap<>();

	private int startTime;
	private int endTime;
	private int timeSpan = 24*3600;
	private Network network;

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

	public void loadNetwork(Network network) {
		this.network = network;
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

		// time-space-diagram for all agents
		Map<Double, Double> agentMap = getTreeMap(event.getVehicleId().toString(), timeSpaceMap);
		Double xPos = CoordUtils.calcEuclideanDistance(network.getNodes().get(Id.createNodeId("1")).getCoord(), network.getLinks().get(event.getLinkId()).getToNode().getCoord());
		agentMap.put(event.getTime(), xPos);
	}

	public Map<Id<Link>, double[]> getVolumes() {
		Map<Id<Link>, double[]> linkVolumesTable = new TreeMap<>();

		for(Map.Entry<Id<Link>, LinkVolumeStat> entry : linksVolumes.entrySet()) {
			double[] tmpVolumes = new double[timeSpan];

			for(Map.Entry<Integer, Double> linkVolumeEntry : entry.getValue().getLinkVolumes().entrySet()) {
				tmpVolumes[linkVolumeEntry.getKey()-startTime] = linkVolumeEntry.getValue();
			}

			int i = 1;
			while(i < tmpVolumes.length) {
				// take previous timestamp's value if value is zero
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

	public Map<String, Map<Double, Double>> getTimeSpace() {
		return timeSpaceMap;
	}

	@Override
	public void reset(int iteration) {
		System.out.println("reset...");
	}

	public Map<String, Map<Double, Double>> getVolumesXY() {
		Map<String, Map<Double, Double>> linkVolumesMap = new TreeMap<>();

		for(Map.Entry<Id<Link>, LinkVolumeStat> entry : linksVolumes.entrySet()) {
			Map<Double, Double> linkIdMap = getTreeMap(entry.getKey().toString(), linkVolumesMap);

			for(Map.Entry<Integer, Double> linkVolumeEntry : entry.getValue().getLinkVolumes().entrySet()) {
				Double time = Double.valueOf(linkVolumeEntry.getKey());
				Double volume = linkVolumeEntry.getValue();
				linkIdMap.put(time, volume);
			}
		}
			return linkVolumesMap;
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

		public Map<Integer, Double> getLinkVolumes() {
			return linkVolumes;
		}
	}

	/**
	 * Gets the map associated with the key in this map, or create an empty map if no mapping exists yet.
	 *
	 * @param key the key of the mapping
	 * @param map the map in which to search
	 * @param <K> type of the in the primary map
	 * @param <C> type of the key in the secondary map
	 * @param <V> type of the values in the secondary map
	 * @return the Map (evt. newly) associated with the key
	 */
	public static <K,C,V> Map<C,V> getTreeMap(
			final K key,
			final Map<K, Map<C,V>> map) {
		Map<C,V> coll = map.get( key );

		if ( coll == null ) {
			coll = new TreeMap<>();
			map.put( key , coll );
		}

		return coll;
	}

}

