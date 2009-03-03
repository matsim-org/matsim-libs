package playground.mmoyo.Validators;

import java.util.List;

import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;

import playground.mmoyo.PTCase2.PTStation;


public class StationValidator {

	public StationValidator(){
		
	}
	
	/*
	 * Validates that all nodes in an intersection have the same coordinate
	 */
	public boolean validateStations(NetworkLayer ptNetworkLayer,PTStation ptStation){
		
		boolean isValid= true;
		for (List<Id> list : ptStation.getIntersecionMap().values()) {
			Id firstId= list.get(0);
			Node firstNode=  ptNetworkLayer.getNode(firstId);
			Coord firstCoord = firstNode.getCoord();
			for (Id id : list){
				Node node=  ptNetworkLayer.getNode(id);
				Coord coord = node.getCoord();
				if(!firstCoord.equals(coord))
					throw new java.lang.NullPointerException(id + "PTNode does not have the same coordinates as their sibling PTNodes ");
			}
		}
		return isValid;
	}
		
}
