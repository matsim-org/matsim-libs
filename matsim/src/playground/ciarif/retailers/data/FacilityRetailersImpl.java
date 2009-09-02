package playground.ciarif.retailers.data;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacility;

//<<<<<<< .mine
//public class FacilityRetailersImpl extends FacilityImpl implements Facility{
//=======
public class FacilityRetailersImpl extends ActivityFacility {
//>>>>>>> .r6943
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected FacilityRetailersImpl(ActivityFacilities layer, Id id, Coord center) {
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
