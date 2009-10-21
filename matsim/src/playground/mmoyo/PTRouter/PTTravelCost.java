package playground.mmoyo.PTRouter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;

/**
 * Calculates the cost of links for the routing algorithm
 */
public class PTTravelCost implements TravelCost{
	private PTTravelTime ptTravelTime;
	double cost;
	double travelTime;
	double travelDistance;
	double waitTime;
	byte aliasType;
	
	////coefficients with original values set to count only travelTime///
	double timeCoeficient =.9;
	double distanceCoeficient=.1;
	double transferPenalty=60;
	double walkCoefficient= 0; 
	double waitCoefficient=0;
	
	public PTTravelCost(final PTTravelTime ptTravelTime) {
		this.ptTravelTime = ptTravelTime;
	}
	
	public PTTravelCost(final PTTravelTime ptTravelTime, double timeCoeficient, double distanceCoeficient, double transferPenalty) {
		this.ptTravelTime = ptTravelTime;
		this.timeCoeficient =timeCoeficient ; 
		this.distanceCoeficient = distanceCoeficient; 
		this.transferPenalty = transferPenalty;
	}

	public double getLinkTravelCost(Link link, double time){
		cost=0;
		travelTime = ptTravelTime.getLinkTravelTime(link, time) ;
		
		/*1.-  for analysis with coefficients///////////////////////////
		travelDistance = link.getLength();
		String type = (( org.matsim.core.network.LinkImpl) link).getType();
		if (type.equals("DetTransfer") || type.equals("Transfer")){
			cost = (travelTime + transferPenalty);
		}else if (type.equals("Standard")){
			cost = (travelTime * timeCoeficient) + (travelDistance * distanceCoeficient) ;
		}else if (type.equals("Access") || type.equals("Egress")){
			cost = travelTime * walkCoefficient;
		}
		*////////////////////////////////////////////////////////////////

		
		//2.- Time as single criterion//////////////////////////////////
		cost= travelTime;
		//////////////////////////////////////////////////////////////*/
		

		//3.- add transfer penalty//////////////////////////
		aliasType = ((PTLink)link).getAliasType();
		if (aliasType == 3 || aliasType == 4 ){  //transfer or dettransfer
			cost += transferPenalty ;
		}
		////////////////////////////////////////////////////////////////*/
	
		return cost;
	}
}