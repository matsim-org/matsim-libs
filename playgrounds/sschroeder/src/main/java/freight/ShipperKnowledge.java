package freight;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import trbFolder.TRBShippersContractGenerator.TimeProfile;




public class ShipperKnowledge {
	
	private Map<Integer, Collection<TimeProfile>> timeProfileMap = new HashMap<Integer, Collection<TimeProfile>>();
	
	public void addTimeProfile(Integer frequency, Collection<TimeProfile> timeProfiles){
		timeProfileMap.put(frequency, timeProfiles);
	}
	
	public Collection<TimeProfile> getTimeProfile(Integer frequency){
		return timeProfileMap.get(frequency);
	}

	public Map<Integer, Collection<TimeProfile>> getTimeProfileMap() {
		return timeProfileMap;
	}
	
	

}
