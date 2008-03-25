package playground.ciarif.retailers;

import java.util.HashMap;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
//import org.matsim.facilities.Facility;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.identifiers.IdI;

public class Retailer {
	private final IdI id;
	private int cust_sqm; // must be a double

	private final HashMap<Integer,Facility> facilities = new HashMap<Integer, Facility>();
	// private final HashMap<IdI,Facility> facilities = new HashMap<IdI, Facility>();

	private final HashMap<IdI,Double> fac_minCusts = new HashMap<IdI,Double>();
	
	private CoordI coord; // NO!

/*	<retailer id="3" cust_sqm="100">
	
	<facility id="1" x="60.0" y="105.0" min_cust_sqm = "100">
		<activity type="shop"/>
		<capacity value="50"/>
		<opentimes day="wkday">
			<opentime start_time="08:00:00" end_time="12:00:00"></opentime>
			<opentime start_time="15:00:00" end_time="19:00:00"></opentime>
		</opentimes>
	</facility>*/
	
	protected Retailer(final IdI id, final int cust_sqm) {
		this.id = id;
		this.cust_sqm = cust_sqm;
	}

	// NO! NEVER create a facility. Find it in the facility DB
	public final Facility createFacility(final Facilities layer, final String a_id, final CoordI center, int min_cust_sqm) {
		// overkill?
		IdI id = new Id(a_id);
		Facility v = new Facility(layer,id,center);
		v.setMin_cust_sqm(min_cust_sqm);
		this.facilities.put(new Integer(a_id),v);
		return v;
	}
	
	public final boolean setFacility(IdI fac_id, Double minCustsqm) {
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

	public final IdI getRetailerId() {
		return this.id;
	}

	public final int getCust_Squm() {
		return this.cust_sqm;
	}

	// NO! Always use IdI for that instead of String or int or similar
//	public final Facility getFacility(final IdI fac_id) {
	public final Facility getFacility(final int h) {
		return this.facilities.get(Integer.valueOf(h));
	}

	public final HashMap<Integer,Facility> getFacilities() {
		return this.facilities;
	}

	// NO! A retailer has no coord!!!
	public void setCoord(final CoordI coord) {
		this.coord = coord;
	}

	// dito
	public CoordI getCoord() {
		return this.coord;
	}

	@Override
	public final String toString() {
		return "[Loc_id=" + this.id + "]" +
		"[cust_sqm=" + this.cust_sqm + "]" +
		"[nof_facilities=" + this.facilities.size() + "]";
	}
}
