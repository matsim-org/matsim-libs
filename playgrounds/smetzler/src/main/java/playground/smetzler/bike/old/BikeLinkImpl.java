package playground.smetzler.bike.old;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
//import playground.smetzler.bike.BikeLink;

public class BikeLinkImpl extends LinkImpl 
implements BikeLink 
{
	private String cycleway;
	private String cyclewaySurface;

	protected BikeLinkImpl(Id<Link> id, Node from, Node to, Network network, double length, double freespeed,
			double capacity, double lanes, String cycleway, String cyclewaySurface) {
		super(id, from, to, network, length, freespeed, capacity, lanes);
		
		this.cycleway = cycleway;
		this.cyclewaySurface = cyclewaySurface;
		// TODO Auto-generated constructor stub
	}
	
	
	@Override
	public String toString() {
		return super.toString() +
		"[cycleway=" + this.cycleway + "]" +
		"[cyclewaySurface=" + this.cyclewaySurface + "]";
		
	}


	@Override
	public String getcycleway() {
		return this.cycleway;
	}


	@Override
	public void setcycleway(String cycleway) {
		this.cycleway = cycleway;
	}


	@Override
	public String getcyclewaySurface() {
		return this.cyclewaySurface;
	}


	@Override
	public void getcyclewaySurface(String cyclewaySurface) {
		this.cyclewaySurface = cyclewaySurface;
	}
}