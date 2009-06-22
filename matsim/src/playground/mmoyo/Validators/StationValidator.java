package playground.mmoyo.Validators;

import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.network.Network;
import playground.mmoyo.input.PTStation;

/**
 * Validates that all nodes in an intersection have the same coordinate and correct id`s
 */
public class StationValidator {
	Network net;
	
	public StationValidator(final Network net){
		this.net = net;
	}
	
	public boolean hasValidCoordinates(final PTStation ptStation){
		for (List<Id> list : ptStation.getIntersecionMap().values()) {
			Id firstId= list.get(0);
			Node firstNode=  net.getNode(firstId);
			Coord firstCoord = firstNode.getCoord();
			for (Id id : list){
				Node node=  net.getNode(id);
				Coord coord = node.getCoord();
				if(!firstCoord.equals(coord))
					throw new java.lang.NullPointerException(id + "PTNode does not have the same coordinates as their sibling PTNodes ");
			}
		}
		return true;
	}
	
	public void validateIds(final Network netDiv){
		int x=0;
		int diferent = 0;
		for (Node node: net.getNodes().values()){
			
			String idStation = ((playground.mmoyo.PTRouter.PTNode)node).getStrIdStation();
			int intId = Integer.valueOf(idStation);

			if (intId< 106699 || intId > 106699){
				diferent++;
				for (Node divNode: netDiv.getNodes().values()){
					if (node.getCoord().equals(divNode.getCoord())){
						System.out.println("Corregible " + x++);
					}
				}
			}
			System.out.println("diferent " + diferent);
			//Coord coord = node.getCoord();
			//find the correct id database;
			//correct it
			//print the changes in screen or log file
		}
	}

	
	
	
}
