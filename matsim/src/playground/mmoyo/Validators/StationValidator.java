package playground.mmoyo.Validators;

import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class StationValidator {

	public StationValidator(){
		
	}
	
	/*
	 * Validates that all nodes in a intersection have the same coordinate
	 */
	public boolean validateStations(NetworkLayer ptNetworkLayer,Map<String, List<IdImpl>>  IntersecionMap){
		boolean isValid= true;
		for (List<IdImpl> list : IntersecionMap.values()) {
			IdImpl firstId= list.get(0);
			Node firstNode=  ptNetworkLayer.getNode(firstId);
			Coord firstCoord = firstNode.getCoord();
			for (IdImpl id : list){
				Node node=  ptNetworkLayer.getNode(id);
				Coord coord = node.getCoord();
				if(!firstCoord.equals(coord))
					throw new java.lang.NullPointerException(id + "PTNode does not have the same coordinates as their sibling PTNodes ");
			}
		}
		return isValid;
	}
	
}
