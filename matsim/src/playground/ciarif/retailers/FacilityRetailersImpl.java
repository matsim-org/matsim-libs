package playground.ciarif.retailers;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.facilities.FacilitiesImpl;
import org.matsim.core.facilities.FacilityImpl;

public class FacilityRetailersImpl extends FacilityImpl implements ActivityFacility {
	
	protected FacilityRetailersImpl(FacilitiesImpl layer, Id id, Coord center) {
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
