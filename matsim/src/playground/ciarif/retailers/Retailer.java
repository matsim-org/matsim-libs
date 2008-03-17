package playground.ciarif.retailers;

import java.util.HashMap;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.identifiers.IdI;

public class Retailer {
	private final IdI locId;
	private int cust_sqm;

	private final HashMap<Integer,Facility> facilities = new HashMap<Integer, Facility>();
	private CoordI coord;


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
	
//	public final Facility createFacility(final String id, final String x, final String y) {
//		return this.createFacility(new Id(id),new Coord(x,y));
//	}

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
