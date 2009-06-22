package playground.mmoyo.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import playground.mmoyo.PTRouter.PTLine;
import playground.mmoyo.PTRouter.PTTimeTable2;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.Coord;

/**
 * Represents a station as a hierarchy with a Main node and so many children nodes as PTLines traveling through it
 */
public class PTStation {
	Map<String, List<Id>> IntersectionMap = new TreeMap<String, List<Id>>(); 
	
	String id;  /**the station id asigned by the transit firm*/
	String stationName;
	Coord coord;
	Id idFareZone;

	public PTStation(PTTimeTable2 ptTimeTable) {
		this.createIntersecionMap(ptTimeTable);
	}
	
	public Map<String, List<Id>> getIntersectionMap() {
		return IntersectionMap;
	}

	public void setIntersectionMap(Map<String, List<Id>> intersectionMap) {
		IntersectionMap = intersectionMap;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getStationName() {
		return stationName;
	}

	public void setStationName(final  String stationName) {
		this.stationName = stationName;
	}

	public Coord getCoord() {
		return coord;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	public Id getIdFareZone() {
		return idFareZone;
	}

	public void setIdFareZone(final Id idFareZone) {
		this.idFareZone = idFareZone;
	}

	public void createIntersecionMap(final PTTimeTable2 ptTimeTable){
		//-> Use idImpl or PTnode types instead of String??
		for (PTLine ptLine : ptTimeTable.getPtLineList()) {
			for (Id idNode : ptLine.getNodeRoute()) {
				String keyNode = getNodeBaseId(idNode);
	    		if (!IntersectionMap.containsKey(keyNode)){
	    			ArrayList<Id> ch = new ArrayList<Id>();
	    			IntersectionMap.put(keyNode, ch);
	    		}
	    		IntersectionMap.get(keyNode).add(idNode);
			}
		}
	}

	public Map<String, List<Id>> getIntersecionMap(){
		return this.IntersectionMap;
	}
	
	/**
	 * Gets a child node with prefix and suffix and returns the Main node Id
	 */
	public String getNodeBaseId(final Id id){
		//-> eliminate this method from networkFactory
		String baseID = id.toString();
		if (baseID.charAt(0)=='_' || baseID.charAt(0)=='~')
			baseID= baseID.substring(1,baseID.length());
		if(Character.isLetter(baseID.charAt(baseID.length()-1))) 	//example of possible node values at intersection:   999, _999, 999b, _999b
			baseID= baseID.substring(0,baseID.length()-1);
		return baseID;
	}
	
	public void print(){
		for(Map.Entry<String, List<Id>> entry: IntersectionMap.entrySet() ){
			System.out.println(entry.getKey() + " " + entry.getValue());
		}
	}
		
}
