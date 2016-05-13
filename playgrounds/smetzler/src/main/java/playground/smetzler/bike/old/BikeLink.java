package playground.smetzler.bike.old;

import org.matsim.api.core.v01.network.Link;


public interface BikeLink extends Link {
	
	public String getcycleway();
	
	public void setcycleway(String cycleway);
	
	public String getcyclewaySurface();
	
	public void getcyclewaySurface(String cyclewaySurface);

}
