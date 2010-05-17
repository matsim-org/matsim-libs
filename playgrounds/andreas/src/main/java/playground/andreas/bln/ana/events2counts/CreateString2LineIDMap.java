package playground.andreas.bln.ana.events2counts;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.transitSchedule.api.TransitSchedule;

public class CreateString2LineIDMap {
	
	private final static Logger log = Logger.getLogger(CreateString2LineIDMap.class);
	
	public static Map<String, Id> createString2LineIDMap(TransitSchedule transitSchedule){
		
		Map<String, Id> string2LineIDMap = new HashMap<String, Id>();
		
		for (Id lineID : transitSchedule.getTransitLines().keySet()) {
			string2LineIDMap.put(lineID.toString(), lineID);
		}
		
		return string2LineIDMap;
	}

}
