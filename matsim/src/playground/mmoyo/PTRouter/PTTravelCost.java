package playground.mmoyo.PTRouter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.network.LinkImpl;

/**
 * Calculates the cost of links for the routing algorithm
 */
public class PTTravelCost implements TravelCost {
	PTTimeTable ptTimeTable;
	String type;
	double cost;
	double travelTime;
	double travelDistance;
	double timeValue = .3;
	double distanceValue =.7;
	double transferValue = 60;
	double walkValue = 1.3;
	
	public PTTravelCost(final PTTimeTable ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
	}

	public double getLinkTravelCost(Link link, double time) {
				
		//////////////set objective values/////////////////////////////////////
		travelTime = ptTimeTable.getLinkTravelTime(link, time) ;
		travelDistance = link.getLength();
		
		//////////////set subjective values ////////////////////////////////////
		type = ((LinkImpl) link).getType();
		if (type.equals("DetTransfer") || type.equals("Transfer")){
			cost = (travelTime + transferValue);
		}else if (type.equals("Standard")){
			cost = (travelTime * timeValue) + (travelDistance * distanceValue) ;
		}else if (type.equals("Access") || type.equals("Egress")){
			cost = travelTime * walkValue;
		}////////////////////////////////////////////////////////////////////////
		
		return cost;
	}
}