package playground.ciarif.retailers;

import java.util.HashMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
//import org.matsim.facilities.Facility;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

public class Retailer {
	private final Id id;
	private int cust_sqm; // must be a double

	private final HashMap<Integer,Facility> facilities = new HashMap<Integer, Facility>();
	// private final HashMap<IdI,Facility> facilities = new HashMap<IdI, Facility>();

	private final HashMap<Id,Double> fac_minCusts = new HashMap<Id,Double>();
	
	protected Retailer(final Id id, final int cust_sqm) {
		this.id = id;
		this.cust_sqm = cust_sqm;
	}

	public final boolean setFacility(Id fac_id, Double minCustsqm) {
		// TODO: implement
		return false;
	}
	
	public final boolean setFacility(org.matsim.facilities.Facility fac, Double minCustsqm) {
		// TODO: implement
		return false;
	}
	
	public final void setCust_sqm(final int cust_sqm) {
		this.cust_sqm = cust_sqm;
	}

	public final Id getRetailerId() {
		return this.id;
	}

	public final int getCust_Squm() {
		return this.cust_sqm;
	}

	// NO! Always use IdI for that instead of String or int or similar
	public final Facility getFacility(final Id fac_id) {
//	public final Facility getFacility(final int h) {
		return this.facilities.get(fac_id);
	}

	public final HashMap<Integer,Facility> getFacilities() {
		return this.facilities;
	}

	@Override
	public final String toString() {
		return "[Loc_id=" + this.id + "]" +
		"[cust_sqm=" + this.cust_sqm + "]" +
		"[nof_facilities=" + this.facilities.size() + "]";
	}
}
