package playground.anhorni.locationchoice.analysis.mc;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Coord;

import playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources.Hectare;

public class CreateTripHectareRelation {
	
	public List<MZTripHectare> createRelations(List<MZTrip> mzTrips, List<Hectare> hectares) {
		List<MZTripHectare> relations = new Vector<MZTripHectare>();
		
		Iterator<MZTrip> mztrips_it = mzTrips.iterator();
		while (mztrips_it.hasNext()) {
			MZTrip mzTrip = mztrips_it.next();
			Hectare hectare = getClosestHectare(mzTrip.getCoordEnd(), hectares);
			relations.add(new MZTripHectare(mzTrip, hectare));
		}
			
		return relations;
	}
	
	
	private Hectare getClosestHectare(Coord coord, List<Hectare> hectares) {
		
		double minDistance = 9999999999999.0;
		Hectare minHectare = null;
		
		Iterator<Hectare> hectare_it = hectares.iterator();
		while (hectare_it.hasNext()) {
			Hectare hectare = hectare_it.next();
			
			double distance = Math.sqrt(
					Math.pow(hectare.getCoords().getX() - coord.getX(),2.0) + 
					Math.pow(hectare.getCoords().getY() - coord.getY(),2.0));
					
			if (distance < minDistance) {
				minDistance = distance;
				minHectare = hectare;
			}
		}	
		return minHectare;
	}
}
