package playground.sergioo.passivePlanning2012.core.population;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;

public abstract class PlaceSharer {
	protected enum Period {
		
		EARLY_MORNING(0, 7*3600-1),
		MORNING_PEAK(7*3600, 10*3600-1),
		BEFORE_LUNCH(10*3600, 13*3600-1),
		AFTER_LUNCH(13*3600, 18*3600-1),
		EVENING_PEAK(18*3600, 21*3600-1),
		NIGHT(21*3600, 24*3600-1);
		
		//Constants
		private static final double PERIODS_TIME = 24*3600;
		
		//Attributes
		private final double startTime;
		private final double endTime;
	
		//Constructors
		private Period(double startTime, double endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
		}
		public static Period getPeriod(double time) {
			for(Period period:Period.values())
				if(period.isPeriod(time))
					return period;
			return null;
		}
		protected boolean isPeriod(double time) {
			time = time%PERIODS_TIME;
			if(startTime<=time && time<=endTime)
				return true;
			return false;
		}
	
	}
	protected class KnownPlace {
		
		//Attributes
		public Id facilityId;
		public SortedMap<Period, Collection<String>> timeTypes = new TreeMap<Period, Collection<String>>();
		protected SortedMap<Period, Map<Id, Double>> travelTimes = new TreeMap<Period, Map<Id, Double>>();
		
		//Constructors
		private KnownPlace(Id facilityId) {
			this.facilityId = facilityId;
		}
		
	}
	
	protected final Set<PlaceSharer> knownPeople = new HashSet<PlaceSharer>();
	protected final Map<Id, KnownPlace> knownPlaces = new HashMap<Id, KnownPlace>();
	
	public Set<PlaceSharer> getKnownPeople() {
		return knownPeople;
	}
	public void addKnownPerson(PlaceSharer placeSharer) {
		knownPeople.add(placeSharer);
	}
	public void addKnownPlace(Id facilityId, double startTime, String typeOfActivity) {
		KnownPlace knownPlace = knownPlaces.get(facilityId);
		if(knownPlace==null) {
			knownPlace = new KnownPlace(facilityId);
			knownPlaces.put(facilityId, knownPlace);
		}
		Collection<String> types = knownPlace.timeTypes.get(Period.getPeriod(startTime));
		if(types==null) {
			types = new ArrayList<String>();
			knownPlace.timeTypes.put(Period.getPeriod(startTime), types);
		}
		types.add(typeOfActivity);
	}
	public void addKnownTravelTime(Id oFacilityId, Id dFacilityId, double startTime, double travelTime) {
		KnownPlace knownPlaceO = knownPlaces.get(oFacilityId);
		if(knownPlaceO==null) {
			knownPlaceO = new KnownPlace(oFacilityId);
			knownPlaces.put(oFacilityId, knownPlaceO);
		}
		KnownPlace knownPlaceD = knownPlaces.get(dFacilityId);
		if(knownPlaceD==null) {
			knownPlaceD = new KnownPlace(dFacilityId);
			knownPlaces.put(dFacilityId, knownPlaceD);
		}
		Map<Id, Double> times = knownPlaceO.travelTimes.get(Period.getPeriod(startTime));
		if(times==null) {
			times = new ConcurrentHashMap<Id, Double>();
			knownPlaceO.travelTimes.put(Period.getPeriod(startTime), times);
		}
		times.put(dFacilityId, travelTime);
	}
	public void shareKnownPlace(Id facilityId, double startTime, String type) {
		for(PlaceSharer placeSharer:knownPeople)
			placeSharer.addKnownPlace(facilityId, startTime, type);
	}
	public void shareKnownTravelTime(Id oFacilityId, Id dFacilityId, double startTime, double travelTime) {
		for(PlaceSharer placeSharer:knownPeople)
			placeSharer.addKnownTravelTime(oFacilityId, dFacilityId, startTime, travelTime);
	}

}
