package playground.mmoyo.PTRouter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.network.LinkImpl;

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
	double timeCoefficient =.85;
	double distanceCoefficient=.15;
	double transferPenalty=60;
	double walkCoefficient= 0; 
	double waitCoefficient=0;
	
	public PTTravelCost(final PTTravelTime ptTravelTime) {
		this.ptTravelTime = ptTravelTime;
	}
	
	public PTTravelCost(final PTTravelTime ptTravelTime, double timeCoefficient, double distanceCoefficient, double transferPenalty) {
		this.ptTravelTime = ptTravelTime;
		this.timeCoefficient =timeCoefficient ; 
		this.distanceCoefficient = distanceCoefficient; 
		this.transferPenalty = transferPenalty;
	}

	public double getLinkTravelCost(Link link, double time){
		cost=0;
		travelTime = ptTravelTime.getLinkTravelTime(link, time) ;
		
		/*1.-  for analysis with coefficients///////////////////////////*/
		travelDistance = link.getLength();
		String type = ((LinkImpl)link).getType();
		if (type.equals("DetTransfer") || type.equals("Transfer")){
			cost = (travelTime + transferPenalty);
		}else if (type.equals("Standard")){
			cost = (travelTime * timeCoefficient) + (travelDistance * distanceCoefficient) ;
		}else if (type.equals("Access") || type.equals("Egress")){
			cost = travelTime * walkCoefficient;
		}
		////////////////////////////////////////////////////////////////

		
		//2.- calculation only with transfer penalty//////////////////////////
		/*
		cost= travelTime;
		aliasType = ((PTLink)link).getAliasType();
		if (aliasType == 3 || aliasType == 4 ){  //transfer or dettransfer
			cost += transferPenalty ;
		}
		/*///////////////////////////////////////////////////////////////*/
	
		return cost;
	}
}