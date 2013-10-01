package playground.wrashid.parkingSearch.ppSim.ttmatrix;

import org.matsim.api.core.v01.Id;

public interface TTMatrix {
	
	double getTravelTime(double time, Id linkId);

}
