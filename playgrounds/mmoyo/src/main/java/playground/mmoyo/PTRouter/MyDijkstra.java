package playground.mmoyo.PTRouter;

import org.matsim.core.network.LinkImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.api.core.v01.network.Network;

/**
 * Implementation of Matsim Dijkstra algorithm adapted to the pt logic network
 */

public class MyDijkstra extends Dijkstra{
	final static String ACCESS = "Access";     			//1
	final static String STANDARD = "Standard";			//2
	final static String TRANSFER = "Transfer";			//3
	final static String DETTRANSFER = "DetTransfer";	//4
	final static String EGRESS = "Egress"; 				//5
	
	public MyDijkstra(final Network network, final TravelCost costFunction, final TravelTime timeFunction) {
		super(network, costFunction, timeFunction);
	}

	/** Validates that the expanded nodes correspond to a valid path*/
	@Override
	protected boolean canPassLink(final Link link) {
		LinkImpl lastLink = (LinkImpl)getData(link.getFromNode()).getPrevLink();
		LinkImpl thisLink = (LinkImpl)link;
		
		boolean pass = false;
		String type= thisLink.getType();
		if (lastLink!=null){
			String lastType = lastLink.getType();
			if (type.equals(DETTRANSFER))  {pass = lastType.equals(STANDARD);}
			else if (type.equals(TRANSFER)){pass = lastType.equals(STANDARD); }
			else if (type.equals(STANDARD)){pass = !lastType.equals(EGRESS);  }
			else if (type.equals(EGRESS))  {pass = lastType.equals(STANDARD); }
       }else{
    	   pass = type.equals(ACCESS);
       }
		return pass;
		
	}

}