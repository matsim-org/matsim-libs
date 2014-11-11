package playground.sergioo.eventAnalysisTools2012;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;

public class CountTimeBins implements LinkEnterEventHandler {
	
	//Constants
	private static final int ONE_HOUR = 3600;
	
	//Attributes
	private Map<String, SortedMap<Integer, Map<Id<Link>, Integer>>> numberOfVehicles = new HashMap<String, SortedMap<Integer, Map<Id<Link>, Integer>>>();
	
	//Methods
	public CountTimeBins(String[] modes, Collection<Id<Link>> linkIds, int totalTime) {
		for(String mode:modes) {
			SortedMap<Integer, Map<Id<Link>, Integer>> modeMap = new TreeMap<Integer, Map<Id<Link>,Integer>>();
			for(int interval = ONE_HOUR; interval<totalTime+ONE_HOUR-1; interval+=ONE_HOUR) {
				Map<Id<Link>, Integer> binMap = new HashMap<Id<Link>, Integer>();
				for(Id<Link> linkId:linkIds)
					binMap.put(linkId, 0);
				modeMap.put(interval, binMap);
			}
			numberOfVehicles.put(mode, modeMap);
		}
	}
	public Map<String, SortedMap<Integer, Map<Id<Link>, Integer>>> getNumberOfVehicles() {
		return numberOfVehicles;
	}
	@Override
	public void reset(int iteration) {
		
	}
	@Override
	public void handleEvent(LinkEnterEvent event) {
		Integer interval = -1;
		for(Integer bin:numberOfVehicles.get(numberOfVehicles.keySet().iterator().next()).keySet())
			if(event.getTime()<bin)
				interval=bin;
		if(event.getVehicleId().toString().startsWith("tr_"))
			numberOfVehicles.get("pt").get(interval).put(event.getLinkId(), numberOfVehicles.get("pt").get(interval).get(event.getLinkId())+1);
		else
			numberOfVehicles.get("car").get(interval).put(event.getLinkId(), numberOfVehicles.get("car").get(interval).get(event.getLinkId())+1);
	}
	
}
