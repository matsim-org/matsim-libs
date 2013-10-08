package playground.sergioo.passivePlanning2012.core.population;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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
	public class KnownPlace {
		
		//Attributes
		private Id facilityId;
		private SortedMap<Period, Set<String>> timeTypes = new TreeMap<Period, Set<String>>();
		protected Map<String, SortedMap<Period, Map<Id, Double>>> travelTimes = new HashMap<String, SortedMap<Period, Map<Id, Double>>>();
		
		//Constructors
		private KnownPlace(Id facilityId) {
			this.facilityId = facilityId;
		}
		
		public Id getFacilityId() {
			return facilityId;
		}
		
		
		public Set<double[]> getTimes(String activityType) {
			Set<double[]> times = new HashSet<double[]>();
			for(Entry<Period, Set<String>> timeType: timeTypes.entrySet())
				if(timeType.getValue().contains(activityType))
					times.add(new double[]{timeType.getKey().startTime, timeType.getKey().endTime});
			return times;
		}
		public Set<String> getActivityTypes() {
			Set<String> activities = new HashSet<String>();
			for(Set<String> timeType: timeTypes.values())
				activities.addAll(timeType);
			return activities;
		}
		public Set<String> getActivityTypes(double time) {
			return timeTypes.get(Period.getPeriod(time));
		}
		public double getTravelTime(String mode, double startTime, Id destinationId) {
			SortedMap<Period, Map<Id, Double>> timess = travelTimes.get(mode);
			if(timess!=null) {
				Map<Id, Double> times = timess.get(Period.getPeriod(startTime));
				if(times!=null) {
					Double time = times.get(destinationId);
					if(time!=null)
						return time;
				}
			}
			return -1;
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
	public Collection<KnownPlace> getKnownPlaces() {
		return knownPlaces.values();
	}
	public KnownPlace getKnownPlace(Id facilityId) {
		return knownPlaces.get(facilityId);
	}
	public void addKnownPlace(Id facilityId, double startTime, String typeOfActivity) {
		KnownPlace knownPlace = knownPlaces.get(facilityId);
		if(knownPlace==null) {
			knownPlace = new KnownPlace(facilityId);
			knownPlaces.put(facilityId, knownPlace);
		}
		Set<String> types = knownPlace.timeTypes.get(Period.getPeriod(startTime));
		if(types==null) {
			types = new HashSet<String>();
			knownPlace.timeTypes.put(Period.getPeriod(startTime), types);
		}
		types.add(typeOfActivity);
	}
	public void addKnownTravelTime(Id oFacilityId, Id dFacilityId, String mode, double startTime, double travelTime) {
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
		SortedMap<Period, Map<Id, Double>> timess = knownPlaceO.travelTimes.get(mode);
		if(timess==null) {
			timess = new TreeMap<PlaceSharer.Period, Map<Id,Double>>();
			knownPlaceO.travelTimes.put(mode, timess);
		}
		Map<Id, Double> times = timess.get(Period.getPeriod(startTime));
		if(times==null) {
			times = new ConcurrentHashMap<Id, Double>();
			timess.put(Period.getPeriod(startTime), times);
		}
		times.put(dFacilityId, travelTime);
	}
	public void shareKnownPlace(Id facilityId, double startTime, String type) {
		for(PlaceSharer placeSharer:knownPeople)
			placeSharer.addKnownPlace(facilityId, startTime, type);
	}
	public void shareKnownTravelTime(Id oFacilityId, Id dFacilityId, String mode, double startTime, double travelTime) {
		for(PlaceSharer placeSharer:knownPeople)
			placeSharer.addKnownTravelTime(oFacilityId, dFacilityId, mode, startTime, travelTime);
	}

}
