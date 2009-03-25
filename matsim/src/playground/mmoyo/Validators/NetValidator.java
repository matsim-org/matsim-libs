package playground.mmoyo.Validators;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.network.Link;
import org.matsim.core.network.NetworkLayer;
import org.matsim.utils.geometry.CoordUtils;

import playground.mmoyo.PTCase2.PTTimeTable2;

public class NetValidator {
	
	private NetworkLayer net;
	private PTTimeTable2 ptTimeTable;
	
	
	public NetValidator (NetworkLayer net, PTTimeTable2 ptTimeTable){
		this.net = net;
		this.ptTimeTable =ptTimeTable; 
	}
	
	public void printNegativeStandardCosts(){
		boolean found=false;
		for(Link link: net.getLinks().values()){
			if(link.getType().equals("Standard")){ 
				double cost= ptTimeTable.GetTravelTime(link);
				if (cost<0){
					System.out.println(link.getId() + " link has negative cost: " + cost);
					found=true;
				}
			} 
		}
		if (!found)
			System.out.println("No negative costs found");
	}

	public void printNegativeTransferCosts(double time){
		int x=0;
		for(Link link: net.getLinks().values()){
			if(link.getType().equals("Transfer")){ 
				double cost= ptTimeTable.GetTransferTime(link, time);
				if (cost<0){
					System.out.println(link.getId() + " link has negative transfer time: " + cost);
					x++;
				}			
			} 
		}
		System.out.println("negative costs found: " + x);
	}
	
	public boolean validLinkLengths(){
		for (Link l : net.getLinks().values()){
			Coord from = l.getFromNode().getCoord();
			Coord to = l.getToNode().getCoord();
			double distance= CoordUtils.calcDistance(from, to);
			if (l.getLength()!= distance ){
				return false;
			}
		}
		return true;
	}
	
	
}
