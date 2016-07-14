package playground.balac.allcsmodestest.facilities;

import java.util.ArrayList;

import org.matsim.api.core.v01.network.Link;

public interface CarSharingStation {
	
public int getNumberOfVehicles();	
	public Link getLink();
	
	public ArrayList<String> getIDs(); 
}
