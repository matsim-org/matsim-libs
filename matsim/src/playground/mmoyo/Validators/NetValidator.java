package playground.mmoyo.Validators;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.mmoyo.PTRouter.PTTimeTable;

/**
 * Validates all links in a network for correct cost values and lengths
 */
public class NetValidator {
	
	private NetworkLayer net;
	private PTTimeTable ptTimeTable;
	
	
	public NetValidator (final NetworkLayer net, final PTTimeTable ptTimeTable){
		this.net = net;
		this.ptTimeTable =ptTimeTable; 
	}
	
	public void printNegativeStandardCosts(){
		boolean found=false;
		for(LinkImpl link: net.getLinks().values()){
			if(link.getType().equals("Standard")){ 
				double cost= ptTimeTable.getLinkTravelTime(link,0);
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
		for(LinkImpl link: net.getLinks().values()){
			if(link.getType().equals("Transfer")){ 
				double cost= ptTimeTable.getTransferTime(link, time);
				if (cost<0){
					System.out.println(link.getId() + " link has negative transfer time: " + cost);
					x++;
				}			
			} 
		}
		System.out.println("negative costs found: " + x);
	}
	
	public boolean validLinkLengths(){
		for (LinkImpl link : net.getLinks().values()){
			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();
			double distance= CoordUtils.calcDistance(from, to);
			if (link.getLength()!= distance ){
				return false;
			}
		}
		return true;
	}
}
