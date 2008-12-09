package playground.ciarif.retailers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.*;

public class Retailers {
	
	private final TreeMap<Id, Facility> retailers = new TreeMap<Id, Facility>();
	private Facilities facilities;
	private final ArrayList<RetailersAlgorithm> algorithms = new ArrayList<RetailersAlgorithm>();
	
	public Retailers(Facilities facilities) {
		this.facilities = facilities;
		
		Iterator fac_iter = this.facilities.getFacilities().values().iterator();
		
		while (fac_iter.hasNext()){
			
			Facility f = (Facility) fac_iter.next();
			System.out.println("  Facilities = " + facilities.getFacilities().keySet());
			Id id = f.getId();//IdImpl id = new IdImpl(8);
			System.out.println("  Id = " + id);
			
			if (f.getActivities().containsKey("shop")) {
				
				this.retailers.put(id,this.facilities.getFacilities().get(id));

			}
			
			System.out.println("  retailers = " + this.retailers);
		}
	}

	public Retailers() {
		// TODO Auto-generated constructor stub
	}

	public TreeMap<Id, Facility> getRetailers() {
		return this.retailers;
	}

	public static Retailers selectRetailersForRelocation(Retailers retailers) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
