package playground.ciarif.flexibletransports.preprocess.stationsLocation;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.network.LinkImpl;

import playground.ciarif.data.LocationPlanner;
import playground.ciarif.flexibletransports.router.CarSharingStations;
import playground.ciarif.stategies.LocationStrategy;

public class CarSharingPlanner implements LocationPlanner {
	
	private LocationStrategy locationStrategy;
	private CarSharingStations csStations;
	
	public CarSharingPlanner () {
		
	}
	
	public CarSharingPlanner (CarSharingStations csStations) {
		
		this.csStations = csStations;
	}
	
	public void init () {
		this.readStations ();
	}


	private void readStations() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void runStrategy(TreeMap<Id, LinkImpl> links) {
		
	    this.locationStrategy.findOptimalLocations(this.csStations, links);
	  }

}
