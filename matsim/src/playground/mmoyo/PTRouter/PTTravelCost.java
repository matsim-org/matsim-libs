package playground.mmoyo.PTRouter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.network.LinkImpl;

/**
 * Calculates the cost of links for the routing algorithm
 */
public class PTTravelCost implements TravelCost {
	private PTTimeTable ptTimeTable;
	private String type;
	double cost;
	double travelTime;
	double travelDistance;

	////coefficients with original values set to count only travelTime///
	double timeCoeficient =.9;
	double distanceCoeficient=.1;
	double transferPenalty=60;
	double walkCoefficient= 0; 
	
	public PTTravelCost(final PTTimeTable ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
	}
	
	public PTTravelCost(final PTTimeTable ptTimeTable, double timeCoeficient, double distanceCoeficient, double transferPenalty) {
		this.ptTimeTable = ptTimeTable; 
		this.timeCoeficient =timeCoeficient ; 
		this.distanceCoeficient = distanceCoeficient ; 
		this.transferPenalty = transferPenalty;
	}

	public double getLinkTravelCost(Link link, double time) {
		cost=0;
		
		////set objective values
		travelTime = ptTimeTable.getLinkTravelTime(link, time) ;
		//travelDistance = link.getLength();
		
		////with Coefficient values
		//cost = (travelTime * timeCoeficient) + (travelDistance * distanceCoeficient) ;

		////Time as only criterion
		//cost= travelTime;
		
		////penalty for changing vehicle
		//type = ((LinkImpl) link).getType();
		//if (type.equals("DetTransfer") || type.equals("Transfer")){
		//	cost = travelTime + transferPenalty;
		//}
		return travelTime;
	}
}