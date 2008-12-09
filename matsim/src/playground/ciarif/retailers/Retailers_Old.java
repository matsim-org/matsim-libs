package playground.ciarif.retailers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordImpl;

// Gruss! balmi
//System.out.println("  reading facilities xml file... ");
//Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
//new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
//System.out.println("  done.");
//
//Iterator<IdI> facid_it =  facilities.getFacilities().keySet().iterator();
//int id = Integer.MIN_VALUE;
//while (facid_it.hasNext()) {
//	int currid = Integer.parseInt(facid_it.next().toString());
//	if (currid > id) { id = currid; }
//}
//id++;
//Facility a_fac = facilities.createFacility(new Id(id),new Coord(0.0,0.0));
//Activity act = a_fac.createActivity("shop");
//act.setCapacity(123);
//act.createOpentime("wkday","08:00:00","18:00:00");
//
//Retailer r = Retailers.getSingleton().createRetailer(new Id(1),123);
//r.setFacility(a_fac,new Double(123.4));


public class Retailers_Old {

	//private static Retailers singleton = new Retailers();
	private String name = null; // probably unnecessary, or include it into schema
	private String desc = null;
	private final TreeMap<Id, Facility> retailers = new TreeMap<Id, Facility>();
	private final ArrayList<RetailersAlgorithm> algorithms = new ArrayList<RetailersAlgorithm>();
	private Facilities facilities;

	public Retailers_Old(Facilities facilities) {
		this.facilities = facilities;
		//facilities.getFacilities().get(key);
		System.out.println("  Facilities = " + facilities.getFacilities().keySet());
		IdImpl id = new IdImpl(8);
		System.out.println("  Facility 8 = " + facilities.getLocation(id));
		this.retailers.put(id,this.facilities.getFacilities().get(id));
	}

//	public static final Retailers getSingleton() {
//		return singleton;
//	}

	public Retailers_Old() {
		// TODO Auto-generated constructor stub
	}

	public final void addAlgorithm(final RetailersAlgorithm algo) {
		this.algorithms.add(algo);
	}

	public final void runAlgorithms() {
		for (int i = 0; i < this.algorithms.size(); i++) {
			RetailersAlgorithm algo = this.algorithms.get(i);
			algo.run(this);
		}
	}

	public final void clearAlgorithms() {
		this.algorithms.clear();
	}

	/**
	 * @param id the id of the retailer
	 * @param cust_spm the numbers of costumers to be served
	 * @return the created Retailer object, or null if it could not be created (maybe because it already exists)
	 */
//	public final Retailer createRetailer(final Id id, final int cust_sqm ) {
//		// check id string for uniqueness
//		if (this.Retailers.containsKey(id)) {
//			return null;
//		}
//		Facility c = new Facility(id, cust_sqm);
//		this.Retailers.put(id, c);
//		return c;
//	}

	public final void setName(final String name) {
		this.name = name;
	}

	public final void setDescription(final String desc) {
		this.desc = desc;
	}

	public ArrayList<RetailersAlgorithm> getAlgorithms() {
		return this.algorithms;
	}

	public final String getName() {
		return this.name;
	}

	public final String getDescription() {
		return this.desc;
	}

	public final TreeMap<Id, Facility> getRetailers() {
		return this.retailers;
	}

	public final Facility getRetailer(final Id locId) {
		return this.retailers.get(locId);
	}

	//needed for testing
//	public static final void reset() {
//		singleton = new Retailers();
//	}

	@Override
	public final String toString() {
		return "[name=" + this.name + "]" +
				"[nof_Retailers=" + this.retailers.size() + "]" +
				"[nof_algorithms=" + this.algorithms.size() + "]";
	}

}
