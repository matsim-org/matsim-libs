package playground.mmoyo.PTRouter;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

public class PTDijkstra extends Dijkstra {

	final String TRANSFERTYPE = "Transfer";
	final String STANDARDTYPE = "Standard";
	final String DETTRANSFERTYPE = "DetTransfer";
	final String WALKINGTYPE = "Walking";
	final String NULLTYPE = null;
	
	boolean ptAlgebra[][] = new boolean[6][6];
	
	public PTDijkstra(Network network, TravelCost costFunction,
			TravelTime timeFunction) {
		super(network, costFunction, timeFunction);

	}

	
	
	protected boolean canPassLink(final Link lastLink, final Link link) {
		boolean pass = false;
		String type = link.getType();
		String lastType = lastLink.getType();
		
		if (type.equals(TRANSFERTYPE)){
			if (lastType.equals(TRANSFERTYPE)) 			pass= false; 		 
			else if (lastType.equals(STANDARDTYPE)) 	pass= true;	 
			else if (lastType.equals(DETTRANSFERTYPE)) 	pass= false;	 
			else if (lastType.equals(WALKINGTYPE)) 		pass= false;		 
			else if (lastType.equals(NULLTYPE)) 		pass= false;			
		}else if (type.equals(WALKINGTYPE)){
			if (lastType.equals(TRANSFERTYPE)) 			pass= false;
			else if (lastType.equals(STANDARDTYPE)) 	pass= true;
			else if (lastType.equals(DETTRANSFERTYPE)) 	pass= false;
			else if (lastType.equals(WALKINGTYPE)) 		pass= false;
			else if (lastType.equals(NULLTYPE)) 		pass= true;
		}else if (type.equals(STANDARDTYPE)){
			if (lastType.equals(TRANSFERTYPE)) 			pass= true;
			else if (lastType.equals(STANDARDTYPE)) 	pass= true;
			else if (lastType.equals(DETTRANSFERTYPE)) 	pass= true;
			else if (lastType.equals(WALKINGTYPE)) 		pass= true;
			else if (lastType.equals(NULLTYPE)) 		pass= false;
		}else if (type.equals(DETTRANSFERTYPE)){
			if (lastType.equals(TRANSFERTYPE)) 			pass= false;
			else if (lastType.equals(STANDARDTYPE)) 	pass= true;
			else if (lastType.equals(DETTRANSFERTYPE)) 	pass= false;
			else if (lastType.equals(WALKINGTYPE)) 		pass= false;
			else if (lastType.equals(NULLTYPE)) 		pass= false;
		}else if (type.equals(null)){
			if (lastType.equals(TRANSFERTYPE)) 			pass = false;
			else if (lastType.equals(STANDARDTYPE)) 	pass = false;
			else if (lastType.equals(DETTRANSFERTYPE)) 	pass = false;
			else if (lastType.equals(WALKINGTYPE)) 		pass = false;
			else if (lastType.equals(NULLTYPE)) 		pass = false;
		}
		
		return pass;
	}
	
	
	
}
