package playground.ciarif.retailers;

import java.util.HashMap;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
//import org.matsim.facilities.Facility;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.identifiers.IdI;

public class Retailer {
	private final IdI locId;
	private int cust_sqm;

	private final HashMap<Integer,Facility> facilities = new HashMap<Integer, Facility>();
	private CoordI coord;

/*	<retailer id="3" cust_sqm="100">
	
	<facility id="1" x="60.0" y="105.0" min_cust_sqm = "100">
		<activity type="shop"/>
		<capacity value="50"/>
		<opentimes day="wkday">
			<opentime start_time="08:00:00" end_time="12:00:00"></opentime>
			<opentime start_time="15:00:00" end_time="19:00:00"></opentime>
		</opentimes>
	</facility>*/
	
	protected Retailer(final IdI locId, final int cust_sqm) {
		this.locId = locId;
		this.cust_sqm = cust_sqm;
	}

	public final Facility createFacility(final Facilities layer, final String a_id, final CoordI center, int min_cust_sqm) {
		// overkill?
		IdI id = new Id(a_id);
		Facility v = new Facility(layer,id,center);
		v.setMin_cust_sqm(min_cust_sqm);
		this.facilities.put(new Integer(a_id),v);
		return v;
	}
	
	public final void setCust_sqm(final int cust_sqm) {
		this.cust_sqm = cust_sqm;
	}

	public final IdI getLocId() {
		return this.locId;
	}

	public final int getCust_Squm() {
		return this.cust_sqm;
	}

	public final Facility getFacility(final int h) {
		return this.facilities.get(Integer.valueOf(h));
	}

	public final HashMap<Integer,Facility> getFacilities() {
		return this.facilities;
	}

	public void setCoord(final CoordI coord) {
		this.coord = coord;
	}

	/** @return Returns the ...
	 **/
	public CoordI getCoord() {
		return this.coord;
	}

	@Override
	public final String toString() {
		return "[Loc_id=" + this.locId + "]" +
		"[cust_sqm=" + this.cust_sqm + "]" +
		"[nof_facilities=" + this.facilities.size() + "]";
	}
}
