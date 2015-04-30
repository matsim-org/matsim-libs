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
import org.matsim.facilities.ActivityFacility;

import playground.sergioo.weeklySimulation.util.misc.Time;

public class PlacesConnoisseur {
	
	public class KnownPlace {
		
		//Attributes
		private Id<ActivityFacility> facilityId;
		private SortedMap<Time.Period, Set<String>> timeTypes = new TreeMap<Time.Period, Set<String>>();
		/*{
			for(Period period:Time.Period.values())
				timeTypes.put(period, new HashSet<String>());
		}*/
		protected Map<String, SortedMap<Time.Period, Map<Id<ActivityFacility>, Double>>> travelTimes = new HashMap<String, SortedMap<Time.Period, Map<Id<ActivityFacility>, Double>>>();
		
		//Constructors
		KnownPlace(Id<ActivityFacility> facilityId) {
			this.facilityId = facilityId;
		}
		
		public Id<ActivityFacility> getFacilityId() {
			return facilityId;
		}
		
		public Set<double[]> getTimes(String activityType) {
			Set<double[]> times = new HashSet<double[]>();
			for(Entry<Time.Period, Set<String>> timeType: timeTypes.entrySet())
				if(timeType.getValue().contains(activityType))
					times.add(new double[]{timeType.getKey().getStartTime(), timeType.getKey().getEndTime()});
			return times;
		}
		public Set<String> getActivityTypes() {
			Set<String> activities = new HashSet<String>();
			for(Set<String> timeType: timeTypes.values())
				activities.addAll(timeType);
			return activities;
		}
		public Set<String> getActivityTypes(double time) {
			Set<String> types = timeTypes.get(Time.Period.getPeriod(time));
			if(types==null) {
				types = new HashSet<String>();
				timeTypes.put(Time.Period.getPeriod(time), types);
			}
			return types;
		}
		public double getTravelTime(String mode, double startTime, Id<ActivityFacility> destinationId) {
			SortedMap<Time.Period, Map<Id<ActivityFacility>, Double>> timess = travelTimes.get(mode);
			if(timess!=null) {
				Map<Id<ActivityFacility>, Double> times = timess.get(Time.Period.getPeriod(startTime));
				if(times!=null) {
					Double time = times.get(destinationId);
					if(time!=null)
						return time;
				}
			}
			return -1;
		}
		
	}
	
	protected final Map<Id<ActivityFacility>, KnownPlace> knownPlaces = new HashMap<Id<ActivityFacility>, KnownPlace>();
	protected boolean areKnownPlacesUsed = false;
	
	public PlacesConnoisseur() {
	}
	
	public boolean areKnownPlacesUsed() {
		return areKnownPlacesUsed;
	}
	public void setAreKnownPlacesUsed(boolean areKnownPlacesUsed) {
		this.areKnownPlacesUsed = areKnownPlacesUsed;
	}
	public Collection<KnownPlace> getKnownPlaces() {
		return knownPlaces.values();
	}
	public KnownPlace getKnownPlace(Id<ActivityFacility> facilityId) {
		return knownPlaces.get(facilityId);
	}
	public void addKnownPlace(Id<ActivityFacility> facilityId, double startTime, String typeOfActivity) {
		KnownPlace knownPlace = knownPlaces.get(facilityId);
		if(knownPlace==null) {
			knownPlace = new KnownPlace(facilityId);
			knownPlaces.put(facilityId, knownPlace);
		}
		Set<String> types = knownPlace.timeTypes.get(Time.Period.getPeriod(startTime));
		if(types==null) {
			types = new HashSet<String>();
			knownPlace.timeTypes.put(Time.Period.getPeriod(startTime), types);
		}
		types.add(typeOfActivity);
	}
	public void addKnownPlace(Id<ActivityFacility> facilityId, double startTime, double endTime, String typeOfActivity) {
		KnownPlace knownPlace = knownPlaces.get(facilityId);
		if(knownPlace==null) {
			knownPlace = new KnownPlace(facilityId);
			knownPlaces.put(facilityId, knownPlace);
		}
		boolean add = false;
		for(Time.Period period:Time.Period.values()) {
			if(period.getStartTime()<=startTime && period.getEndTime()>=startTime)
				add = true;
			if(add) {
				Set<String> types = knownPlace.timeTypes.get(period);
				if(types==null) {
					types = new HashSet<String>();
					knownPlace.timeTypes.put(period, types);
				}
				types.add(typeOfActivity);
			}
			if(period.getStartTime()<=endTime && period.getEndTime()>=endTime)
				add = false;
		}
	}
	public void addKnownPlace(KnownPlace knownPlace) {
		KnownPlace knownPlace2 = knownPlaces.get(knownPlace.getFacilityId());
		if(knownPlace2==null)
			knownPlaces.put(knownPlace.getFacilityId(), knownPlace);
		else
			for(Entry<Time.Period, Set<String>> entry:knownPlace.timeTypes.entrySet()) {
				Set<String> acts = knownPlace2.timeTypes.get(entry.getKey());
				if(acts == null)
					knownPlace2.timeTypes.put(entry.getKey(), entry.getValue());
				else
					for(String act:entry.getValue())
						acts.add(act);
			}
	}
	public void addKnownTravelTime(Id<ActivityFacility> oFacilityId, Id<ActivityFacility> dFacilityId, String mode, double startTime, double travelTime) {
		KnownPlace knownPlaceO = knownPlaces.get(oFacilityId);
		if(knownPlaceO!=null) {
			KnownPlace knownPlaceD = knownPlaces.get(dFacilityId);
			if(knownPlaceD!=null) {
				SortedMap<Time.Period, Map<Id<ActivityFacility>, Double>> timess = knownPlaceO.travelTimes.get(mode);
				if(timess==null) {
					timess = new TreeMap<Time.Period, Map<Id<ActivityFacility>, Double>>();
					knownPlaceO.travelTimes.put(mode, timess);
				}
				Map<Id<ActivityFacility>, Double> times = timess.get(Time.Period.getPeriod(startTime));
				if(times==null) {
					times = new ConcurrentHashMap<Id<ActivityFacility>, Double>();
					timess.put(Time.Period.getPeriod(startTime), times);
				}
				times.put(dFacilityId, travelTime);
			}
		}
	}

}
