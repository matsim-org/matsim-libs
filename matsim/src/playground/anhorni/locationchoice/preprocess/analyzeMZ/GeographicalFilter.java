package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.api.basic.v01.Id;

import playground.anhorni.locationchoice.cs.helper.MZTrip;

public class GeographicalFilter {
		
	public List<MZTrip> filterTrips(List<MZTrip> mzTrips) {
		
		Vector<MZTrip> filteredTrips = new Vector<MZTrip>(); 
		
		Iterator<MZTrip> mztrips_it = mzTrips.iterator();
		while (mztrips_it.hasNext()) {
			MZTrip mztrip = mztrips_it.next();

			if (mztrip.getCoordEnd().getX() > 1000 && mztrip.getCoordEnd().getY() > 1000 
					&& mztrip.getCoordStart().getX() > 1000 && mztrip.getCoordStart().getY() > 1000) {
				filteredTrips.add(mztrip);	
			}
		}
		return filteredTrips;
	}
	
	public TreeMap<Id, PersonTrips> filterPersons(TreeMap<Id, PersonTrips> personTrips) {
		
		TreeMap<Id, PersonTrips> filteredPersons = new TreeMap<Id, PersonTrips>(); 
		
		Iterator<PersonTrips> personTrips_it = personTrips.values().iterator();
		while (personTrips_it.hasNext()) {
			PersonTrips pt = personTrips_it.next();
			if (!pt.containsImplausibleCoordinates()) {
				filteredPersons.put(pt.getPersonId(), pt);
			}
		}
		return filteredPersons;
	}	
}
