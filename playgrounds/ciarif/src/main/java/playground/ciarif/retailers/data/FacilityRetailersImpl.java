package playground.ciarif.retailers.data;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;

public class FacilityRetailersImpl extends ActivityFacilityImpl {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected FacilityRetailersImpl(ActivityFacilitiesImpl layer, Id id, Coord center) {
		super(id, center, null);
	}

	private int score;
	
	public int getScore (){
		return this.score;
	}
	
	public void setScore (int score) {
		this.score = score;
	}
}
