package playground.balac.onewaycarsharingredisgned.qsim;

import org.matsim.api.core.v01.network.Link;

public class OneWayCarsharingRDStation  {

	
	private Link link;
	private int numberOfVehicles;
	
	public OneWayCarsharingRDStation(Link link, int numberOfVehicles) {
		
		this.link = link;
		this.numberOfVehicles = numberOfVehicles;
	}
	
	public int getNumberOfVehicles() {
		
		return numberOfVehicles;
	}
	
	public Link getLink() {
		
		return link;
	}
	
}