package playground.sergioo.passivePlanning2012.core.population;

import java.util.HashSet;
import java.util.Set;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.facilities.ActivityFacility;

public abstract class PlaceSharer extends PlacesConnoisseur {
	
	protected final Set<PlaceSharer> knownPeople = new HashSet<PlaceSharer>();
	private double shareProbability = 1;
	
	public PlaceSharer() {
	}
	
	public void addKnownPerson(PlaceSharer placeSharer) {
		knownPeople.add(placeSharer);
	}
	public void setShareProbability(double shareProbability) {
		if(shareProbability>0 && shareProbability<=1)
			this.shareProbability = shareProbability;
	}
	public void shareKnownPlace(Id<ActivityFacility> facilityId, double startTime, String type) {
		for(PlaceSharer placeSharer:knownPeople)
			if(MatsimRandom.getRandom().nextDouble()<shareProbability && !placeSharer.areKnownPlacesUsed)
				placeSharer.addKnownPlace(facilityId, startTime, type);
	}
	public void shareKnownTravelTime(Id<ActivityFacility> oFacilityId, Id<ActivityFacility> dFacilityId, String mode, double startTime, double travelTime) {
		for(PlaceSharer placeSharer:knownPeople)
			placeSharer.addKnownTravelTime(oFacilityId, dFacilityId, mode, startTime, travelTime);
	}

}
