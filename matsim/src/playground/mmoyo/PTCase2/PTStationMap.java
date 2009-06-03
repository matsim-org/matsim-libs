package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mmoyo.PTRouter.PTLine;

/*
 * a map containing all nodes with same coordinates in order to create transfer links between them
 */

public class PTStationMap {

	Map<String, List<Id>> IntersectionMap = new TreeMap<String, List<Id>>(); 
		
	public PTStationMap(PTTimeTable2 ptTimeTable) {
		this.createIntersecionMap(ptTimeTable);
	}
	
	public void createIntersecionMap(PTTimeTable2 ptTimeTable){
		//-> eliminate this method from factory
		for (PTLine ptLine : ptTimeTable.getPtLineList()) {
			for (String strIdNode: ptLine.getRoute()) {
				insertNode(strIdNode);
			}
		}
	}

	public void insertNode(String strIdNode){
		String strNodeBaseId =  getNodeBaseId(strIdNode);
		if (!IntersectionMap.containsKey(strNodeBaseId)){
			List<Id> ch = new ArrayList<Id>();
			IntersectionMap.put(strNodeBaseId, ch);
		}
		IntersectionMap.get(strNodeBaseId).add(new IdImpl(strIdNode));
	}
	

	public Map<String, List<Id>> getIntersecionMap(){
		return this.IntersectionMap;
	}
	
	public String getNodeBaseId(String strId){
		//-> eliminate this method from networkFactory and PTStation
		String baseID = strId;
		if (baseID.charAt(0)=='_' || baseID.charAt(0)=='~')
			baseID= baseID.substring(1,baseID.length());
		if(Character.isLetter(baseID.charAt(baseID.length()-1))) 	//example of possible node values at intersection:   999, _999, 999b, _999b
			baseID= baseID.substring(0,baseID.length()-1);
		return baseID;
	}
		
}
