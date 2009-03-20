package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import playground.anhorni.locationchoice.cs.helper.MZTrip;

public class GroceryFilter {
		
	public List<MZTrip> filterTrips(List<MZTrip> mzTrips) {
		
		Vector<MZTrip> filteredTrips = new Vector<MZTrip>(); 
		
		Iterator<MZTrip> mztrips_it = mzTrips.iterator();
		while (mztrips_it.hasNext()) {
			MZTrip mztrip = mztrips_it.next();
			if (mztrip.getPurpose().equals("1")) {
				filteredTrips.add(mztrip);
			}	
		}
		return filteredTrips;
	}
}
