package playground.mmoyo.PTCase2;

import org.matsim.network.Link;
import org.matsim.router.util.TravelTime;

public class PTTravelTime implements TravelTime {
	public final double WALKING_SPEED = 0.836;  //   0.836 m/s [Al-Azzawi2007] ?      1.34 m/s [Antonini2004]? 
	private PTTimeTable2 ptTimeTable;
	
	public PTTravelTime(PTTimeTable2 ptTimeTable) {
		this.ptTimeTable = ptTimeTable; 
	}
	
	public double getLinkTravelTime(Link link, double time) {
		double travelTime =0;

		if (link.getType().equals("Transfer")){
			travelTime= transferTime(link,time); 

			
		}else if (link.getType().equals("Walking")){
			travelTime = link.getLength()* WALKING_SPEED;
		
			
		}else if (link.getType().equals("Standard")){
			travelTime= ptTimeTable.GetTravelTime(link);
			//travelTime= 1;
		
			
		}else if (link.getType().equals("DetTransfer")){
			double walkTime=link.getLength()* WALKING_SPEED;
			Link nextLink= getNextLink(link);
			double waitingTime= transferTime(nextLink, (time+walkTime)); 
			travelTime= walkTime + waitingTime;
		}

		//travelTime= link.getLength();
		return travelTime;
	}
	
	
	
	private double transferTime(Link link, double t){
		double transTime= ptTimeTable.GetTransferTime(link, t);
		if (t<0) {
			System.out.println("link:" + link + " travelTime:" + t);
			throw new java.lang.ArithmeticException("negative transfer time value");
			//throw new java.lang.NullPointerException(dtLink.getId() + "DetLink has no valid outLinks");
		}
		transTime=1;
		return transTime;
	}
	
	private Link getNextLink(Link dtLink){
		Link retLink=null;
		int numStandards =0;
		for (Link outLink : dtLink.getToNode().getOutLinks().values()){
			if (outLink.getType().equals("Standard")){
				numStandards++;
				return outLink; 
			}
		}
		if (numStandards>1){
			throw new java.lang.NullPointerException(dtLink.getId() + "DetLink has no valid outLinks");
		}
		if (retLink.equals(null)){
			throw new java.lang.NullPointerException("Error finding next link");
		}
		
		return retLink;
	}
	
	
}