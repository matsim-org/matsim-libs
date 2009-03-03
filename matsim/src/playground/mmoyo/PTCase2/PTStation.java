package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.NetworkLayer;

import playground.mmoyo.PTRouter.PTLine;
import playground.mmoyo.Validators.StationValidator;

public class PTStation {

	Map<String, List<Id>> IntersectionMap = new TreeMap<String, List<Id>>(); 
		
	
	public PTStation(PTTimeTable2 ptTimeTable) {
		this.createIntersecionMap(ptTimeTable);
	}
	
	//-> eliminate this method from factory
	public void createIntersecionMap(PTTimeTable2 ptTimeTable){
		for (PTLine ptLine : ptTimeTable.getPtLineList()) {
			for (String strIdNode: ptLine.getRoute()) {
				String strNodeBaseId =  getNodeBaseId(strIdNode);
				if (!IntersectionMap.containsKey(strNodeBaseId)){
	    			List<Id> ch = new ArrayList<Id>();
	    			IntersectionMap.put(strNodeBaseId, ch);
	    		}
	    		IntersectionMap.get(strNodeBaseId).add(new IdImpl(strIdNode));
			}
		}
	}


	public Map<String, List<Id>> getIntersecionMap(){
		return this.IntersectionMap;
	}
	
	//-> eliminate this method from factory
	public String getNodeBaseId(String strId){
		String baseID = strId;
		if (baseID.charAt(0)=='_' || baseID.charAt(0)=='~')
			baseID= baseID.substring(1,baseID.length());
		if(Character.isLetter(baseID.charAt(baseID.length()-1))) 	//example of possible node values at intersection:   999, _999, 999b, _999b
			baseID= baseID.substring(0,baseID.length()-1);
		return baseID;
	}
	
}
