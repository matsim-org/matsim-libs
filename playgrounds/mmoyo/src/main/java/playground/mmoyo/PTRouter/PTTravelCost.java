package playground.mmoyo.PTRouter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.TravelCost;

/**
 * Calculates the cost of links for the routing algorithm
 */
public class PTTravelCost implements TravelCost{
	private PTTravelTime ptTravelTime;
	
	public PTTravelCost(final PTTravelTime ptTravelTime) {
		this.ptTravelTime = ptTravelTime;
	}
	
	public double getLinkGeneralizedTravelCost(Link link, double time){
		double cost = ptTravelTime.getLinkTravelTime(link, time) ;  

		String type = ((LinkImpl)link).getType();
		if (type.equals( PTValues.DETTRANSFER_STR ) || type.equals( PTValues.TRANSFER_STR )){
			cost = (cost* PTValues.walkCoefficient)+ PTValues.transferPenalty;
			//cost = (cost + PTValues.transferPenalty) * PTValues.timeCoefficient;
			//cost = (cost * 	PTValues.timeCoefficient) + (link.getLength() * PTValues.distanceCoefficient);
		}else if (type.equals( PTValues.STANDARD_STR )){
			cost = (cost * 	PTValues.timeCoefficient) + (link.getLength() * PTValues.distanceCoefficient);
		}else if (type.equals( PTValues.ACCESS_STR ) || type.equals( PTValues.EGRESS_STR )){
			cost = cost * PTValues.walkCoefficient; 
		}else{
			throw new java.lang.RuntimeException("the pt link does not have a defined type: " + link.getId());
		}
		return cost;
	}
}