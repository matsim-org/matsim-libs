package playground.mmoyo.Validators;

import org.matsim.network.NetworkLayer;
import org.matsim.network.Link;

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
	
	
	
	
}
