package playground.mmoyo.Validators;

import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

/**
 * Validates that all nodes in an intersection have the same coordinate and correct id`s
 */
public class StationValidator {
	NetworkLayer net;
	
	public StationValidator(final NetworkLayer net){
		this.net = net;
	}
	
	public void validateIds(final NetworkLayer netDiv){
		int x=0;
		int differents = 0;
		for (NodeImpl node: net.getNodes().values()){
			
			String idStation = ((playground.mmoyo.PTRouter.PTNode)node).getStrIdStation();
			int intId = Integer.valueOf(idStation);

			if (intId< 106699 || intId > 106699){
				differents++;
				for (NodeImpl divNode: netDiv.getNodes().values()){
					if (node.getCoord().equals(divNode.getCoord())){
						System.out.println("Corregible " + x++);
					}
				}
			}
			System.out.println("different " + differents);
			//Coord coord = node.getCoord();
			//find the correct id database;
			//correct it
			//print the changes in screen or log file
		}
	}

	
	
	
}
