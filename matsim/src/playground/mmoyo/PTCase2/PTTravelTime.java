package playground.mmoyo.PTCase2;

import org.matsim.core.api.network.Link;
import org.matsim.core.router.util.TravelTime;
import playground.mmoyo.Validators.CostValidator;

public class PTTravelTime implements TravelTime {
	public final double WALKING_SPEED = 0.836;  //   0.836 m/s [Al-Azzawi2007]?      1.34 m/s [Antonini2004]? 
	private PTTimeTable2 ptTimeTable;
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
			travelTime = link.getLength()* WALKING_SPEED;
		
		}else if (type.equals("Standard")){
			travelTime= ptTimeTable.GetTravelTime(link);
		
		}else if (type.equals("DetTransfer")){
			double walkTime=link.getLength()* WALKING_SPEED;
			
			System.out.println(ptTimeTable.getNextLinkMap().size());
			Link nextLink = ptTimeTable.getNextLinkMap().get(link.getId());
			
			double waitingTime= transferTime(nextLink, (time+walkTime));
			double nextLinkTravelTime = ptTimeTable.GetTravelTime(nextLink);


			return walkTime + waitingTime - nextLinkTravelTime; 

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