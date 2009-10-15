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
	/*
	final static String ACCESS = "Access";     			//1
	final static String STANDARD = "Standard";			//2
	final static String TRANSFER = "Transfer";			//3
	final static String DETTRANSFER = "DetTransfer";	//4
	final static String EGRESS = "Egress"; 				//5
	*/
	byte aliasType =0;
	byte lastType =0;
	
	public MyDijkstra(final Network network, final TravelCost costFunction, final TravelTime timeFunction) {
		super(network, costFunction, timeFunction);
	}

	/** Validates that the expanded nodes correspond to a valid path*/
	protected boolean canPassLink(final Link link) {
		PTLink prevLink = (PTLink)getData(link.getFromNode()).getPrevLink();
		aliasType = ((PTLink)link).getAliasType();	
		
		boolean pass= false;
		if	(prevLink!=null){
			lastType = prevLink.getAliasType();
			
			switch (aliasType){
				case 4:    //detTransfer
					pass = (lastType ==2);  
					break;
				case 3:    //transfer
					pass = (lastType ==2);
					break;
				case 2:    //standard
					pass = (lastType!=5);
					break;
				case 5:    //egress
					pass = (lastType ==2);
					break;
			}
			//System.out.println(prevLink.getType() + " " + ptLink.getType() + " " + pass);
		}else{ //access
			pass = (aliasType==1);
		}

		return pass;
	
			
			/*
		return false;
		boolean pass = false;
		String type= thisLink.getType();
		if (lastLink!=null){
			String lastType = lastLink.getType();
			if (type.equals(DETTRANSFER))  {pass = lastType.equals(STANDARD);}
			else if (type.equals(TRANSFER)){pass = lastType.equals(STANDARD); }
			else if (type.equals(STANDARD)){pass = !lastType.equals(EGRESS);  }
			else if (type.equals(EGRESS))  {pass = lastType.equals(STANDARD); }
		return pass;
		*/
	}

}