package playground.balac.twowaycarsharingredisigned.qsim;

import org.matsim.api.core.v01.network.Link;

public class TwoWayCSStation  {

	
	private Link link;
	private int numberOfVehicles;
	
	public TwoWayCSStation(Link link, int numberOfVehicles) {
		
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