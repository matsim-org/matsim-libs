package playground.mmoyo.PTCase2;

import org.matsim.core.api.network.Link;
import org.matsim.core.router.util.TravelTime;
import playground.mmoyo.Validators.CostValidator;

public class PTTravelTime implements TravelTime {
	double walkSpeed =  Walk.walkingSpeed();
	private PTTimeTable2 ptTimeTable = new PTTimeTable2();
	public CostValidator costValidator = new CostValidator();
	
	
	public PTTravelTime(PTTimeTable2 ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
	}
	
	private double transferTime(Link link, double t){
		double transTime= ptTimeTable.GetTransferTime(link, t);
		if (transTime<0) {
			costValidator.pushNegativeValue(link.getId(), t, transTime);
			transTime = 600;
		}
		return transTime;  //->> Transfer factor
	}
	
	public double getLinkTravelTime(Link link, double time) {
		double travelTime=0;
		String type = link.getType();
		
		if (type.equals("Transfer")){
			travelTime= transferTime(link,time); 
		}else if (type.equals("Walking")){
			travelTime= link.getLength()* walkSpeed;
		
		}else if (type.equals("Standard")){
			travelTime= ptTimeTable.GetTravelTime(link);
		
		}else if (type.equals("DetTransfer")){
			double walkTime=link.getLength()* walkSpeed;
			Link nextLink = ptTimeTable.getNextLinkMap().get(link.getId());
			double waitingTime= transferTime(nextLink, (time+walkTime));
			double nextLinkTravelTime = ptTimeTable.GetTravelTime(nextLink);
			travelTime= walkTime + waitingTime - nextLinkTravelTime; 
			
			//double waitingTime= transferTime(link, (time+walkTime));			
			//Fix this
			//Link nextLink= getNextLink(link);  this was moved to ptlink
			//double waitingTime= transferTime(nextLink, (time+walkTime)); 
			//travelTime= walkTime + waitingTime;
			
			//-> pre-process this before and save it somewhere
			/*
			int numStandards =0;
			for (Link outLink : link.getToNode().getOutLinks().values()) {
				if (outLink.getType().equals("Standard")){
					numStandards++;
					double waitingTime= transferTime(outLink, (time+walkTime));
					travelTime= walkTime + waitingTime;
				}
			}
			
			if (numStandards>1)
				throw new java.lang.NullPointerException(link.getId() + "DetLink has no valid outLinks");
			travelTime= walkTime+ 600;
		*/

		}
		return travelTime;
	}
}