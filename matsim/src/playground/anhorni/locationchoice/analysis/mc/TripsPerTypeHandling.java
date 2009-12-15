package playground.anhorni.locationchoice.analysis.mc;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

public class TripsPerTypeHandling {
	
	private TreeMap<String, TripMeasureDistribution> shopTrips= new TreeMap<String, TripMeasureDistribution>();
	private TreeMap<String, TripMeasureDistribution> leisureTrips = new TreeMap<String, TripMeasureDistribution>();
	private TreeMap<String, TripMeasureDistribution> workTrips= new TreeMap<String, TripMeasureDistribution>();
	private TreeMap<String, TripMeasureDistribution> educationTrips = new TreeMap<String, TripMeasureDistribution>();
	
	
	public TripsPerTypeHandling() {
		shopTrips.put("pt", new TripMeasureDistribution("pt", "shop",  new Vector<MZTrip>()));
		shopTrips.put("car", new TripMeasureDistribution("car", "shop",  new Vector<MZTrip>()));
		shopTrips.put("walk", new TripMeasureDistribution("walk", "shop",  new Vector<MZTrip>()));
		shopTrips.put("bike", new TripMeasureDistribution("bike", "shop",  new Vector<MZTrip>()));
		
		leisureTrips.put("pt", new TripMeasureDistribution("pt", "leisure",  new Vector<MZTrip>()));
		leisureTrips.put("car", new TripMeasureDistribution("car", "leisure",  new Vector<MZTrip>()));
		leisureTrips.put("walk", new TripMeasureDistribution("walk", "leisure",  new Vector<MZTrip>()));
		leisureTrips.put("bike", new TripMeasureDistribution("bike", "leisure",  new Vector<MZTrip>()));	
		
		workTrips.put("pt", new TripMeasureDistribution("pt", "work",  new Vector<MZTrip>()));
		workTrips.put("car", new TripMeasureDistribution("car", "work",  new Vector<MZTrip>()));
		workTrips.put("walk", new TripMeasureDistribution("walk", "work",  new Vector<MZTrip>()));
		workTrips.put("bike", new TripMeasureDistribution("bike", "work",  new Vector<MZTrip>()));	
		
		educationTrips.put("pt", new TripMeasureDistribution("pt", "education",  new Vector<MZTrip>()));
		educationTrips.put("car", new TripMeasureDistribution("car", "education",  new Vector<MZTrip>()));
		educationTrips.put("walk", new TripMeasureDistribution("walk", "education",  new Vector<MZTrip>()));
		educationTrips.put("bike", new TripMeasureDistribution("bike", "education",  new Vector<MZTrip>()));
	}
	
	public void createDistributions(List<MZTrip> mzTrips) {		
		Iterator<MZTrip> mzTrips_it = mzTrips.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();

			if (mzTrip.getPurpose().equals("shop")) {
				shopTrips.get(mzTrip.getMatsimMode()).addTrip(mzTrip);
			}
			else if (mzTrip.getPurpose().equals("leisure")) {
				leisureTrips.get(mzTrip.getMatsimMode()).addTrip(mzTrip);
			}
			else if (mzTrip.getPurpose().equals("work")) {
				workTrips.get(mzTrip.getMatsimMode()).addTrip(mzTrip);
			}
			else if (mzTrip.getPurpose().equals("education")) {
				educationTrips.get(mzTrip.getMatsimMode()).addTrip(mzTrip);
			}
		}
	}

	public TreeMap<String, TripMeasureDistribution> getShopTrips() {
		return shopTrips;
	}

	public void setShopTrips(
			TreeMap<String, TripMeasureDistribution> shopTrips) {
		this.shopTrips = shopTrips;
	}

	public TreeMap<String, TripMeasureDistribution> getLeisureTrips() {
		return leisureTrips;
	}

	public void setLeisureTrips(
			TreeMap<String, TripMeasureDistribution> leisureTrips) {
		this.leisureTrips = leisureTrips;
	}

	public TreeMap<String, TripMeasureDistribution> getWorkTrips() {
		return workTrips;
	}

	public void setWorkTrips(
			TreeMap<String, TripMeasureDistribution> workTrips) {
		this.workTrips = workTrips;
	}

	public TreeMap<String, TripMeasureDistribution> getEducationTrips() {
		return educationTrips;
	}

	public void setEducationTrips(
			TreeMap<String, TripMeasureDistribution> educationTrips) {
		this.educationTrips = educationTrips;
	}
}
