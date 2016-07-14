package playground.smetzler.bike.old;

import org.matsim.core.network.Link;


public interface BikeLink extends Link {
	
	public String getcycleway();
	
	public void setcycleway(String cycleway);
	
	public String getcyclewaySurface();
	
	public void getcyclewaySurface(String cyclewaySurface);

}
