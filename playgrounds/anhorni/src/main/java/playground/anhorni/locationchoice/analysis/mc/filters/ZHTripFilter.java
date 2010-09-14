package playground.anhorni.locationchoice.analysis.mc.filters;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.anhorni.locationchoice.analysis.mc.MZTrip;

public class ZHTripFilter {
	
	private double radius = 30.0 * 1000.0;
	private CoordImpl center = new CoordImpl(683518.0,246836.0);	
	
	public List<MZTrip> filterRegion(List<MZTrip> trips) {
		List<MZTrip> filteredTrips = new Vector<MZTrip>();	
		Iterator<MZTrip> trips_it = trips.iterator();
		while (trips_it.hasNext()) {
			MZTrip trip = trips_it.next();
			
			if (this.intersect(trip, radius, center)) {
				filteredTrips.add(trip);
			}
		}
		return filteredTrips;
	}	
	
	private boolean intersect(MZTrip mzTrip, double radius, CoordImpl center) {
		
		double distance = CoordUtils.distancePointLinesegment(
				mzTrip.getCoordStart(), mzTrip.getCoordEnd(), center);
		
		if (distance <= radius) return true;
		return false;
	}
}
