package playground.smetzler.bike;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
//import playground.smetzler.bike.BikeLink;

public class BikeLinkImpl extends LinkImpl {
	private String cycleway;
	private String cyclewaySurface;

	protected BikeLinkImpl(Id<Link> id, Node from, Node to, Network network, double length, double freespeed,
			double capacity, double lanes, String cycleway, String cyclewaySurface) {
		super(id, from, to, network, length, freespeed, capacity, lanes);
		
		this.cycleway = cycleway;
		this.cyclewaySurface = cyclewaySurface;
		// TODO Auto-generated constructor stub
	}
	
	
//	@Override
//	public String toString() {
//		return super.toString() +
//		"[id=" + this.getId() + "]" +
//		"[from_id=" + this.from.getId() + "]" +
//		"[to_id=" + this.to.getId() + "]" +
//		"[length=" + this.length + "]" +
//		"[freespeed=" + this.freespeed + "]" +
//		"[capacity=" + this.capacity + "]" +
//		"[permlanes=" + this.nofLanes + "]" +
//		"[origid=" + this.origid + "]" +
//		"[type=" + this.type + "]";
//		"[cycleway=" + this.cycleway + "]";
//		"[cyclewaySurface=" + this.cyclewaySurface + "]";
//		
//	}

}

