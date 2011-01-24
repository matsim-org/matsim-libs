package playground.mzilske.freight.vrp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

public class Tours {
	private static Logger logger = Logger.getLogger(Tours.class);
	
	private ArrayList<VehicleTourImpl> tours = new ArrayList<VehicleTourImpl>();
	
	private Map<Id,VehicleTourImpl> nodesToTourMap = new HashMap<Id, VehicleTourImpl>();

	public ArrayList<VehicleTourImpl> getTours() {
		return tours;
	}

	public Map<Id, VehicleTourImpl> getNodesToTourMap() {
		return nodesToTourMap;
	}
	
	public void removeTour(VehicleTour tour){
		if(tours.contains(tour)){
			for(Node n : tour.getNodes()){
				nodesToTourMap.remove(n.getId());
			}
			tours.remove(tour);
		}	
	}
	
	public void addTour(VehicleTourImpl tour){
		for(Node n : tour.getNodes()){
			nodesToTourMap.put(n.getId(), tour);
		}
		tours.add(tour);
	}
}
