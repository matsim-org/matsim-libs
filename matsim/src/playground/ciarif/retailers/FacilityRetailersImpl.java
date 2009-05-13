package playground.ciarif.retailers;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;

public class FacilityRetailersImpl extends ActivityFacilityImpl implements ActivityFacility {
	
	protected FacilityRetailersImpl(ActivityFacilitiesImpl layer, Id id, Coord center) {
		super(layer, id, center);
		// TODO Auto-generated constructor stub
	}

	private int score;
	
	public int getScore (){
		return this.score;
	}
	
	public void setScore (int score) {
		this.score = score;
	}
}
