package playground.mmoyo.zz_archive.PTRouter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

/**
 * Implementation of Matsim Dijkstra algorithm adapted to the pt logic network
 */

public class MyDijkstra extends Dijkstra{

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
			if (type.equals(PTValues.DETTRANSFER_STR))  {pass = lastType.equals(PTValues.STANDARD_STR); }
			else if (type.equals(PTValues.TRANSFER_STR)){pass = lastType.equals(PTValues.STANDARD_STR); }
			else if (type.equals(PTValues.STANDARD_STR)){pass = !lastType.equals(PTValues.EGRESS_STR);  }
			else if (type.equals(PTValues.EGRESS_STR))  {pass = lastType.equals(PTValues.STANDARD_STR); }
       }else{
    	   pass = type.equals(PTValues.ACCESS_STR);
       }
		return pass;
	}
}