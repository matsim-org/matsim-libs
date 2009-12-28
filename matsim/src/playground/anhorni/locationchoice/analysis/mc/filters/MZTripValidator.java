package playground.anhorni.locationchoice.analysis.mc.filters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.anhorni.locationchoice.analysis.mc.MZTrip;
import playground.anhorni.locationchoice.analysis.mc.io.ZPReader;

public class MZTripValidator {
	
	private final static Logger log = Logger.getLogger(MZTripValidator.class);
	private HashSet<Id> removedTripsFromPersons = new HashSet<Id>();
	private List<Id> dateFilteredPersonIds ;
	
	public MZTripValidator() {
		ZPReader zpReader = new ZPReader();
		this.dateFilteredPersonIds = zpReader.read("input/MZ/Zielpersonen.dat");
	}
	
	public List<MZTrip> filterTrips(List<MZTrip> trips) {
		
		List<MZTrip> tripsFiltered = new Vector<MZTrip>();	
		
		log.info("Number of trips before date filtering: " + trips.size());		
		tripsFiltered = this.filterTripsByDate(dateFilteredPersonIds, trips);
		
		log.info("Number of trips before removing implausible ones: " + tripsFiltered.size());
		tripsFiltered = this.removeImplausibleTrips(tripsFiltered);
		
		log.info("Filtering done; Number of trips: " + tripsFiltered.size());
		return tripsFiltered;	
	}
			
	// M0-FR
	private List<MZTrip> filterTripsByDate(List<Id> dateFilteredPersonIds, List<MZTrip> trips) {
		List<MZTrip> tripsFiltered = new Vector<MZTrip>();	
		
		Iterator<MZTrip> mzTrips_it = trips.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			if (this.validDate(mzTrip, dateFilteredPersonIds)) {
				tripsFiltered.add(mzTrip);	
			}
			else {
				this.removedTripsFromPersons.add(mzTrip.getPersonId());
			}
		}
		return tripsFiltered;
	}
	
	private List<MZTrip> removeImplausibleTrips(List<MZTrip> trips) {
		List<MZTrip> tripsFiltered = new Vector<MZTrip>();
		
		Iterator<MZTrip> mzTrips_it = trips.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			
			if (this.implausiblyLongTrip(mzTrip)) {
				this.removedTripsFromPersons.add(mzTrip.getPersonId());
				continue;
			}
			if (this.implausiblyShortTrip(mzTrip)) {		// filters also round trips
				this.removedTripsFromPersons.add(mzTrip.getPersonId());
				continue;
			}
			if (!this.plausibleCoordinates(mzTrip)) {
				this.removedTripsFromPersons.add(mzTrip.getPersonId());
				continue;
			}	
			if (!this.modeDefined(mzTrip)) {
				this.removedTripsFromPersons.add(mzTrip.getPersonId());
				continue;
			}	
			tripsFiltered.add(mzTrip);	
		}
		return tripsFiltered;
	}
	
	private boolean validDate(MZTrip mzTrip, List<Id> dateFilteredPersonIds) {
		if (dateFilteredPersonIds.contains(mzTrip.getPersonId())) {
			return true;
		}
		return false;
	}
		
	private boolean plausibleCoordinates(MZTrip mzTrip) {		
		Coord coordStart = mzTrip.getCoordStart();
		Coord coordEnd = mzTrip.getCoordEnd();
		
		if (coordStart.getX() > 1000 && coordStart.getY() > 1000 
				&& coordEnd.getX() > 1000 && coordEnd.getY() > 1000) {
			return true;
		}
		else return false;
	}
	
	private boolean implausiblyLongTrip(MZTrip mzTrip) {		
		double distance = mzTrip.getCoordStart().calcDistance(mzTrip.getCoordEnd());
		
		if (mzTrip.getMatsimMode().equals("walk") && distance > 30000) {
			return true;
		}
		else return false;
	}
	
	// includes also round trips!
	private boolean implausiblyShortTrip(MZTrip mzTrip) {				
		if (mzTrip.getCoordStart().calcDistance(mzTrip.getCoordEnd()) < 1.0) {
			return true;
		}
		else return false;
	}
	
	private boolean modeDefined(MZTrip mzTrip) {
		if (mzTrip.getMatsimMode().equals("undefined")) {
			return false;
		}
		else return true;
	}
	
	public HashSet<Id> getRemovedTripsFromPersons() {
		return removedTripsFromPersons;
	}
	
		
/*	private List<MZTrip>  filterImplausible(List<MZTrip> trips) {
		List<MZTrip> filteredTrips = new Vector<MZTrip>();
		
		Iterator<MZTrip> trips_it = trips.iterator();
		while (trips_it.hasNext()) {
			MZTrip trip = trips_it.next();
			String type = trip.getShopOrLeisure();
			
			// WALK
			if (trip.getMatsimMode().equals("walk") && type.equals("shop")) {
				if (trip.getCoordStart().calcDistance(trip.getCoordEnd()) <= 3000 &&
						trip.getCoordStart().calcDistance(trip.getCoordEnd()) >= 50) { 
					filteredTrips.add(trip);
				}
			}
			if (trip.getMatsimMode().equals("walk") && type.equals("leisure")) {
				if (trip.getCoordStart().calcDistance(trip.getCoordEnd()) <= 25000 && 
						trip.getCoordStart().calcDistance(trip.getCoordEnd()) >= 50) { 
					filteredTrips.add(trip);
				}
			}
			
			// BIKE
			if (trip.getMatsimMode().equals("bike") && type.equals("shop")) {
				if (trip.getCoordStart().calcDistance(trip.getCoordEnd()) <= 6000 &&
						trip.getCoordStart().calcDistance(trip.getCoordEnd()) >= 100) { 
					filteredTrips.add(trip);
				}
			}
			if (trip.getMatsimMode().equals("bike") && type.equals("leisure")) {
				if (trip.getCoordStart().calcDistance(trip.getCoordEnd()) <= 50000 &&
						trip.getCoordStart().calcDistance(trip.getCoordEnd()) >= 100) { 
					filteredTrips.add(trip);
				}
			}
			
			// PT
			if (trip.getMatsimMode().equals("pt") && type.equals("shop")) {
				if (trip.getCoordStart().calcDistance(trip.getCoordEnd()) <= 200000 &&
						trip.getCoordStart().calcDistance(trip.getCoordEnd()) >= 100) { 
					filteredTrips.add(trip);
				}
			}
			if (trip.getMatsimMode().equals("pt") && type.equals("leisure")) {
				if (trip.getCoordStart().calcDistance(trip.getCoordEnd()) <= 300000 &&
						trip.getCoordStart().calcDistance(trip.getCoordEnd()) >= 100) { 
					filteredTrips.add(trip);
				}
			}
			
			// CAR
			if (trip.getMatsimMode().equals("car") && type.equals("shop")) {
				if (trip.getCoordStart().calcDistance(trip.getCoordEnd()) <= 200000 &&
						trip.getCoordStart().calcDistance(trip.getCoordEnd()) >= 100) { 
					filteredTrips.add(trip);
				}
			}
			if (trip.getMatsimMode().equals("car") && type.equals("leisure")) {
				if (trip.getCoordStart().calcDistance(trip.getCoordEnd()) <= 300000 &&
						trip.getCoordStart().calcDistance(trip.getCoordEnd()) >= 100) { 
					filteredTrips.add(trip);
				}
			}
		}
		return filteredTrips;
	}*/
	
	/*	public boolean isValid(MZTrip mzTrip) {
	if (!this.validDate(mzTrip, dateFilteredPersonIds)) {
		return false;	
	}
	if (this.implausiblyLongTrip(mzTrip)) {
		return false;
	}
	if (this.implausiblyShortTrip(mzTrip)) { // filters also round trips
		return false;
	}
	if (!this.plausibleCoordinates(mzTrip)) {
		return false;
	}	
	if (!this.modeDefined(mzTrip)) {
		return false;
	}	
	return true;
}*/
	

}
