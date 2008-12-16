package playground.ciarif.retailers;

import java.util.*;
import java.lang.Object;
import java.math.*;


import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.*;
import org.matsim.gbl.MatsimRandom;

public class Retailers {
	
	private final Map<Id,Retailer> retailers = new LinkedHashMap<Id, Retailer>();

	private final ArrayList<RetailersAlgorithm> algorithms = new ArrayList<RetailersAlgorithm>();
	
	public final boolean addRetailer(final Retailer retailer) {
		if (retailer == null) { return false; }
		if (this.retailers.containsKey(retailer.getId())) { return false; }
		this.retailers.put(retailer.getId(),retailer);
		return true;
	}
	
	public Map<Id,Retailer> getRetailers() {
		return this.retailers;
	}

//	public static Retailers selectRetailersForRelocation(Retailers retailers, double percentage) {
//		Retailers retailersForRelocation = new Retailers();
//		double nr= percentage * retailers.getRetailers().size()/100;
//		long numberRetailers = Math.round (nr);
//		Iterator<Facility> ret_iter = retailers.getRetailers().values().iterator();
//		Vector<Facility> ret = new Vector<Facility>();
//		
//		while (ret_iter.hasNext()){
//			ret.add(ret_iter.next());
//		}
//		int ii =0;
//		
//		while (ii<numberRetailers) {
//			
//			int randomKey = MatsimRandom.random.nextInt(ret.size());
//						retailersForRelocation.getRetailers().put(new IdImpl(ii), ret.get(randomKey));
//			ret.remove(randomKey);
//			ii=ii+1;
//		}
//		//System.out.println("  retailers_a = " + retailersForRelocation.getRetailers().keySet());
//		return retailersForRelocation;
//	}

	
}
