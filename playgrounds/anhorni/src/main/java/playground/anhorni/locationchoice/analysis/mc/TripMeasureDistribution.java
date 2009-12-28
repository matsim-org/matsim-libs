package playground.anhorni.locationchoice.analysis.mc;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.world.WorldUtils;

import playground.anhorni.locationchoice.preprocess.helper.Utils;

public class TripMeasureDistribution {
	
	private String purpose;
	private String mode;
	private List<MZTrip> trips = new Vector<MZTrip>();
	private TreeMap<String, List<MZTrip>> regionalTrips = new TreeMap<String, List<MZTrip>>();
	
	private boolean filter99 = false;
	
	public TripMeasureDistribution(String mode, String purpose, List<MZTrip> trips) {
		this.mode = mode;	
		this.purpose = purpose;
		this.trips = trips;
		this.regionalTrips.put("ch", new Vector<MZTrip>());
	}
	
	public void addTrip(MZTrip trip) {
		this.trips.add(trip);
		this.regionalTrips.get("ch").add(trip);
	}
		
	public void filterRegion(String region) {
		List<MZTrip> filteredTrips = new Vector<MZTrip>();	
		Iterator<MZTrip> trips_it = trips.iterator();
		while (trips_it.hasNext()) {
			MZTrip trip = trips_it.next();
			
			double radius = 0.0;
			CoordImpl center = null;
			
			if (region.equals("zh")) {
				radius = 30000.0;
				center = new CoordImpl(683518.0,246836.0);	
			}
			else if (region.equals("cityzh")) {
				radius = 10000.0;
				center = new CoordImpl(683518.0,246836.0);	
			}
			if (this.intersect(trip, radius, center)) {
				filteredTrips.add(trip);
			}
		}
		if (this.regionalTrips.get(region) == null) {
			this.regionalTrips.remove(region);
			
		}
		this.regionalTrips.put(region, filteredTrips);
	}	
	
	// in our simulation there are only shopping facilities inside zh circle
	private boolean intersect(MZTrip mzTrip, double radius, CoordImpl center) {		
		double distance = WorldUtils.distancePointLinesegment(
				mzTrip.getCoordStart(), mzTrip.getCoordEnd(), center);
		
		if (distance <= radius) return true;
		return false;
	}
	
	public int getNumberOfTrips(String region) {		
		int numberOfTrips = 0;
		if (filter99) {
			Iterator<MZTrip> trips_it = this.regionalTrips.get(region).iterator();
			while (trips_it.hasNext()) {
				MZTrip trip = trips_it.next();
				if (Integer.parseInt(trip.getPurposeCode()) > -99) {
					numberOfTrips++;
				}
			}
			return numberOfTrips;
		}
		else {	
			return this.regionalTrips.get(region).size();
		}
	}

	public List<MZTrip> getRegionalTrips(String region) {
		return this.regionalTrips.get(region);
	}
	
	public double getMinDist(String region) {
		double min = 999999999999999999999.0;
		Iterator<MZTrip> trips_it = this.regionalTrips.get(region).iterator();
		while (trips_it.hasNext()) {
			MZTrip trip = trips_it.next();
			
			if (Integer.parseInt(trip.getPurposeCode()) < 0 && filter99) continue;
			
			CoordImpl coordStart = trip.getCoordStart();
			CoordImpl coordEnd = trip.getCoordEnd();
			double dist = coordStart.calcDistance(coordEnd)/1000.0;
	
			if (dist < min) {
				min = dist;
			}
		}
		return min;
	}
	
	public double getMinDur(String region) {
		double min = 999999999999999999999.0;
		Iterator<MZTrip> trips_it = this.regionalTrips.get(region).iterator();
		while (trips_it.hasNext()) {
			MZTrip trip = trips_it.next();
			if (Integer.parseInt(trip.getPurposeCode()) < 0 && filter99) continue;
			double dur = trip.getDuration();
			if (dur < min) {
				min = dur;
			}
		}
		return min;
	}
	
	public double getMaxDist(String region) {
		double max = 0.0;
		Iterator<MZTrip> trips_it = this.regionalTrips.get(region).iterator();
		while (trips_it.hasNext()) {
			MZTrip trip = trips_it.next();
			
			if (Integer.parseInt(trip.getPurposeCode()) < 0 && filter99) continue;
			
			CoordImpl coordStart = trip.getCoordStart();
			CoordImpl coordEnd = trip.getCoordEnd();
			double dist = coordStart.calcDistance(coordEnd)/1000.0;
			if (dist > max) {
				max = dist;
			}
		}
		return max;	
	}
	
	public double getMaxDur(String region) {
		double max = 0.0;
		Iterator<MZTrip> trips_it = this.regionalTrips.get(region).iterator();
		while (trips_it.hasNext()) {
			MZTrip trip = trips_it.next();
			
			if (Integer.parseInt(trip.getPurposeCode()) < 0 && filter99) continue;
			double dur = trip.getDuration();
			if (dur > max) {
				max = dur;
			}
		}
		return max;	
	}
	
	public double getAvgDist(String region) {
		double avg = 0.0;
		Iterator<MZTrip> trips_it = this.regionalTrips.get(region).iterator();
		while (trips_it.hasNext()) {
			MZTrip trip = trips_it.next();
			
			if (Integer.parseInt(trip.getPurposeCode()) < 0 && filter99) continue;
			
			CoordImpl coordStart = trip.getCoordStart();
			CoordImpl coordEnd = trip.getCoordEnd();
			double dist = coordStart.calcDistance(coordEnd)/1000.0;
			avg += dist / this.getNumberOfTrips(region);
		}
		return avg;
	}
	
	public double getAvgDur(String region) {
		double avg = 0.0;
		Iterator<MZTrip> trips_it = this.regionalTrips.get(region).iterator();
		while (trips_it.hasNext()) {
			MZTrip trip = trips_it.next();
			
			if (Integer.parseInt(trip.getPurposeCode()) < 0 && filter99) continue;

			double dur = trip.getDuration();
			avg += dur / this.getNumberOfTrips(region);
		}
		return avg;
	}
	
	public double getMedianDist(String region) {

		List<Double> values = new Vector<Double>();
		Iterator<MZTrip> trips_it = this.regionalTrips.get(region).iterator();
		while (trips_it.hasNext()) {
			MZTrip trip = trips_it.next();
			
			if (Integer.parseInt(trip.getPurposeCode()) < 0 && filter99) continue;
			
			CoordImpl coordStart = trip.getCoordStart();
			CoordImpl coordEnd = trip.getCoordEnd();
			double dist = coordStart.calcDistance(coordEnd)/1000.0;
			values.add(dist);
		}
		return Utils.median(values);
	}
	
	public double getMedianDur(String region) {

		List<Double> values = new Vector<Double>();
		Iterator<MZTrip> trips_it = this.regionalTrips.get(region).iterator();
		while (trips_it.hasNext()) {
			MZTrip trip = trips_it.next();
			
			if (Integer.parseInt(trip.getPurposeCode()) < 0 && filter99) continue;
			
			double dist = trip.getDuration();
			values.add(dist);
		}
		return Utils.median(values);
	}

	public boolean isFilter99() {
		return filter99;
	}

	public void setFilter99(boolean filter99) {
		this.filter99 = filter99;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}	
}
