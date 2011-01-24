/**
 * 
 */
package playground.mzilske.freight.vrp;

import java.util.List;

public class VRPSolution {
	List<VehicleTourImpl> tours;
	
	public VRPSolution(List<VehicleTourImpl> tours) {
		super();
		this.tours = tours;
	}

	public List<VehicleTourImpl> getTours() {
		return tours;
	}

	
}
