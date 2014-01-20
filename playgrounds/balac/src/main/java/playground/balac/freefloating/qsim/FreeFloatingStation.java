package playground.balac.freefloating.qsim;

import org.matsim.api.core.v01.network.Link;

public class FreeFloatingStation {

	
	private Link link;
	private int numberOfVehicles;
	
	public FreeFloatingStation(Link link, int numberOfVehicles) {
		
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
