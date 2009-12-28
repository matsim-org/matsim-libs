package playground.anhorni.locationchoice.analysis.mc;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
//import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.anhorni.locationchoice.analysis.mc.filters.ZHTripFilter;
import playground.anhorni.locationchoice.preprocess.helper.Utils;


public class PersonTripActs {
	
	private Id personId;
	private List<MZTrip> mzTripsAllTypesUnFiltered = new Vector<MZTrip>();
	private List<MZTrip> mzTripsAllTypes = new Vector<MZTrip>();
	private String [] modes = {"pt", "car", "bike", "walk"};
	
	//private final static Logger log = Logger.getLogger(PersonTripActs.class);
	
	public PersonTripActs(Id id) {
		this.personId = id;
	}
	
	public void addTrip(MZTrip mzTrip) {
		this.mzTripsAllTypes.add(mzTrip);
		this.mzTripsAllTypesUnFiltered.add(mzTrip);
	}
	
	public Id getPersonId() {
		return personId;
	}
	public void setPersonId(Id personId) {
		this.personId = personId;
	}
	
	public boolean isValid() {	
		String [] modes = {"pt", "car", "bike", "walk"};
		for (int i = 0; i < modes.length; i++) {
			if (this.getDurationAllActsAllModesAggregated() > 24.0 * 3600) {
				return false;
			}
		}
		return true;		
	}
	
	public void undoZHRegionFiltering() {
		this.mzTripsAllTypes.clear();
		Iterator<MZTrip> mzTrips_it = mzTripsAllTypesUnFiltered.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			this.mzTripsAllTypes.add(mzTrip);
		}
	}
	
	public void filterZHRegion() {
		ZHTripFilter zhTripFilter = new ZHTripFilter();
		this.mzTripsAllTypes = zhTripFilter.filterRegion(this.mzTripsAllTypes);
	}	
	

	public TreeMap<String, List<Double>> getLeisureActDuration() {
		
		TreeMap<String, List<Double>> budgets = new TreeMap<String, List<Double>>();			
		for (int i = 0; i < modes.length; i++) {
			budgets.put(modes[i], new Vector<Double>());
		}
		double endTimePreviousTrip = 0.0;
		String purposePreviousTrip = "home";
		String modePreviousTrip = "undefined";
		int tripNr = 0;
		
		Iterator<MZTrip> mzTrips_it = mzTripsAllTypes.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			// act. was not home
			if (tripNr > 0 && (purposePreviousTrip.equals("leisure") &&
					(mzTrip.getCoordStart().calcDistance(mzTrip.getHome()) > 0.1))) {
				
				String key = modePreviousTrip;
				budgets.get(key).add(mzTrip.getStartTime() - endTimePreviousTrip);			
			}
			modePreviousTrip = Utils.convertModeMZ2Plans(Integer.parseInt(mzTrip.getWmittel()));
			endTimePreviousTrip = mzTrip.getEndTime();
			purposePreviousTrip = mzTrip.getPurpose();
			tripNr++;
		}
		return budgets;
	}
	
	public TreeMap<String, List<Double>> getTripDistancePerType(String type) {
		
		TreeMap<String, List<Double>> distances = new TreeMap<String, List<Double>>();				
		for (int i = 0; i < modes.length; i++) {
			distances.put(modes[i], new Vector<Double>());
		}
		
		Iterator<MZTrip> mzTrips_it = mzTripsAllTypes.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			if (mzTrip.getPurpose().equals(type)) {				
				String key = Utils.convertModeMZ2Plans(Integer.parseInt(mzTrip.getWmittel()));
				distances.get(key).add(mzTrip.getCoordEnd().calcDistance(mzTrip.getCoordStart()));			
			}
		}
		return distances;
	}
			
	public TreeMap<String, Double> getActDurationPerTypeAggregated(String type) {
		
		TreeMap<String, Double> budgets = new TreeMap<String, Double>();			
		for (int i = 0; i < modes.length; i++) {
			budgets.put(modes[i], 0.0);
		}
		double endTimePreviousTrip = 0.0;
		String purposePreviousTrip = "home";
		String modePreviousTrip = "undefined";
		int tripNr = 0;
		
		Iterator<MZTrip> mzTrips_it = mzTripsAllTypes.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			// act. was not home
			if (tripNr > 0 && (purposePreviousTrip.equals(type) &&
					(mzTrip.getCoordStart().calcDistance(mzTrip.getHome()) > 0.1))) {
				
				String key = modePreviousTrip;
				double oldVal = budgets.get(key);
				budgets.remove(key);
				budgets.put(key, oldVal + mzTrip.getStartTime() - endTimePreviousTrip);			
			}
			modePreviousTrip = Utils.convertModeMZ2Plans(Integer.parseInt(mzTrip.getWmittel()));
			endTimePreviousTrip = mzTrip.getEndTime();
			purposePreviousTrip = mzTrip.getPurpose();
			tripNr++;
		}
		return budgets;
	}
	
	public TreeMap<String, Double> getTripDistancePerTypeAggregated(String type) {
		TreeMap<String, Double> distances = new TreeMap<String, Double>();			
		for (int i = 0; i < modes.length; i++) {
			distances.put(modes[i], 0.0);
		}
		
		Iterator<MZTrip> mzTrips_it = mzTripsAllTypes.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			if (mzTrip.getPurpose().equals(type)) {
				
				String key = Utils.convertModeMZ2Plans(Integer.parseInt(mzTrip.getWmittel()));
				double oldVal = distances.get(key);
				distances.remove(key);
				distances.put(key, oldVal + mzTrip.getCoordEnd().calcDistance(mzTrip.getCoordStart()));				
			}
		}
		return distances;
	}
		
	public TreeMap<String, Double> getTripDistancesAllTypesAggregated() {

		TreeMap<String, Double> distances = new TreeMap<String, Double>();			
		for (int i = 0; i < modes.length; i++) {
			distances.put(modes[i], 0.0);
		}
		
		Iterator<MZTrip> mzTrips_it = mzTripsAllTypes.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			
			String key = Utils.convertModeMZ2Plans(Integer.parseInt(mzTrip.getWmittel()));
			double oldVal = distances.get(key);
			distances.remove(key);
			distances.put(key, oldVal + mzTrip.getCoordEnd().calcDistance(mzTrip.getCoordStart()));	
		}
		return distances;
	}
	
	public double getDurationAllActsAllModesAggregated() {
		
		double endTimePreviousTrip = 0.0;
		int tripNr = 0;
		double durationAllActs = 0.0;
		
		Iterator<MZTrip> mzTrips_it = mzTripsAllTypes.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			// act. was not home
			if (tripNr > 0 && (mzTrip.getCoordStart().calcDistance(mzTrip.getHome()) > 0.1)) {
				durationAllActs += mzTrip.getStartTime() - endTimePreviousTrip;		
			}
			endTimePreviousTrip = mzTrip.getEndTime();
			tripNr++;
		}
		return durationAllActs;
	}
}
