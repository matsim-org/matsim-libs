package playground.ciarif.retailers;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.algorithms.CountsAlgorithm;
import org.matsim.utils.identifiers.IdI;

public class Retailers {

	private static Retailers singleton = new Retailers();
	private String name = null;
	private String desc = null;
	private int year = 0;
	private String layer = null;
	private final TreeMap<IdI, Retailer> Retailers = new TreeMap<IdI, Retailer>();
	private final ArrayList<RetailersAlgorithm> algorithms = new ArrayList<RetailersAlgorithm>();
	

	private Retailers() {
	}

	public static final Retailers getSingleton() {
		return singleton;
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
	 * @param locId
	 * @param csId
	 * @return the created Retailer object, or null if it could not be created (maybe because it already exists)
	 */
	public final Retailer createRetailer(final IdI locId, final int cust_sqm ) {
		// check id string for uniqueness
		if (this.Retailers.containsKey(locId)) {
			return null;
		}
		Retailer c = new Retailer(locId, cust_sqm);
		this.Retailers.put(locId, c);
		return c;
	}

	public final void setName(final String name) {
		this.name = name;
	}

	public final void setDescription(final String desc) {
		this.desc = desc;
	}

	public final void setYear(final int year) {
		this.year = year;
	}

	public final void setLayer(final String layer) {
		this.layer = layer;
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

	public final int getYear() {
		return this.year;
	}

	public final String getLayer() {
		return this.layer;
	}

	public final TreeMap<IdI, Retailer> getRetailers() {
		return this.Retailers;
	}

	public final Retailer getRetailer(final IdI locId) {
		return this.Retailers.get(locId);
	}

	//needed for testing
	public static final void reset() {
		singleton = new Retailers();
	}

	@Override
	public final String toString() {
		return "[name=" + this.name + "]" +
				"[nof_Retailers=" + this.Retailers.size() + "]" +
				"[nof_algorithms=" + this.algorithms.size() + "]";
	}

}
