package playground.andreas.bln.ana.events2counts;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class CreateString2LineIDMap {
	
	public static Map<String, Id<TransitLine>> createString2LineIDMap(TransitSchedule transitSchedule){
		
		Map<String, Id<TransitLine>> string2LineIDMap = new HashMap<>();
		
		for (Id<TransitLine> lineID : transitSchedule.getTransitLines().keySet()) {
			string2LineIDMap.put(lineID.toString(), lineID);
		}
		
		return string2LineIDMap;
	}

}
