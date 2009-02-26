package playground.mmoyo.PTCase2;

import org.matsim.interfaces.core.v01.Link;
import org.matsim.router.util.TravelTime;
import playground.mmoyo.Validators.CostValidator;

public class PTTravelTime implements TravelTime {
	public final double WALKING_SPEED = 0.836;  //   0.836 m/s [Al-Azzawi2007] ?      1.34 m/s [Antonini2004]? 
	private PTTimeTable2 ptTimeTable;
	public CostValidator costValidator = new CostValidator();
	
	public PTTravelTime(PTTimeTable2 ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
	}
	
	private double transferTime(Link link, double t){
		double transTime= ptTimeTable.GetTransferTime(link, t);
		if (transTime<0) {
			costValidator.pushNegativeValue(link.getId(), t, transTime);
			transTime = 1;
			
			/*
			System.out.println("link:" + link + " travelTime:" + t);
			throw new java.lang.ArithmeticException("negative transfer time value");
			//throw new java.lang.NullPointerException(dtLink.getId() + "DetLink has no valid outLinks");
			*/
		}
		transTime=600;
		return transTime;
	}
	
	public double getLinkTravelTime(Link link, double time) {
		double travelTime =0;
		String type = link.getType();
		
		if (type.equals("Transfer")){
			 travelTime= transferTime(link,time); 
		
			
		}else if (type.equals("Walking")){
			travelTime = link.getLength()* WALKING_SPEED;
		
			
		}else if (type.equals("Standard")){
			travelTime= ptTimeTable.GetTravelTime(link);
			
			
		}else if (type.equals("DetTransfer")){
			double walkTime=link.getLength()* WALKING_SPEED;
			/* Fix this
			Link nextLink= getNextLink(link);
			double waitingTime= transferTime(nextLink, (time+walkTime)); 
			travelTime= walkTime + waitingTime;
			*/
			int numStandards =0;
			double waitingTime=0;
			for (Link outLink : link.getToNode().getOutLinks().values()) {
				if (outLink.getType().equals("Standard")){
					numStandards++;
					
					waitingTime= transferTime(outLink, (time+walkTime));
					travelTime= walkTime + waitingTime;
					/*
					waitingTime= ptTimeTable.GetTransferTime(outLink, (time+walkTime) );
					if (waitingTime<0) {waitingTime= 0;} ///
					travelTime= walkTime + waitingTime;
					*/
				}
			}
			if (numStandards>1)
				throw new java.lang.NullPointerException(link.getId() + "DetLink has no valid outLinks");
			travelTime= walkTime+ 600;
		}

		//travelTime=  link.getLength();
		return travelTime;
	}
	
	
	/*
	private Link getNextLink(Link dtLink){
		Link retLink = null;
		if (dtLink.getType().equals("DetTransfer")){
			int numStandards =0;
			for (Link outLink : dtLink.getToNode().getOutLinks().values()){
				if (outLink.getType().equals("Standard")){
					numStandards++;
					retLink= outLink; 
				}
			}
			if (numStandards>1){
				throw new java.lang.NullPointerException(dtLink.getId() + "DetLink has no valid outLinks");
			}
			if (retLink==null){
				throw new java.lang.NullPointerException("Error finding next link");
			}
		}
		return retLink;
	}
	*/
	
}