package playground.toronto;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;

public class FlagTurnLinks {

	public static void run(Network network){
		for (Link link : network.getLinks().values()){
			LinkImpl l = (LinkImpl) link;
			String fromN = link.getFromNode().getId().toString();
			String toN = link.getToNode().getId().toString();
			
			if (fromN.contains("-") && toN.contains("-")){
				//both node-ends are virtual nodes created during ManneuverCreation
				if (fromN.split("-")[0].equals(toN.split("-")[0]))
					l.setType(TorontoLinkTypes.turn); //only flag links whose from and to nodes were created from the same one
			}
		}
	}
	
}
