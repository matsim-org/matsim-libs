package playground.smetzler.bike;

import org.matsim.api.core.v01.network.Link;


public interface BikeLink extends Link {
	
	public double getcycleway();
	
	public void setcycleway(String cycleway);
	
	public double getcyclewaySurface();
	
	public void getcyclewaySurface(String cyclewaySurface);

}
