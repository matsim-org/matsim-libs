package playground.balac.allcsmodestest.facilities;

import java.util.ArrayList;

import org.matsim.core.network.Link;

public interface CarSharingStation {
	
public int getNumberOfVehicles();	
	public Link getLink();
	
	public ArrayList<String> getIDs(); 
}
