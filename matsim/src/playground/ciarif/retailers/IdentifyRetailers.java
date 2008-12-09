package playground.ciarif.retailers;

import java.util.Iterator;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;

public class IdentifyRetailers extends RetailersAlgorithm {
	
	private Retailers_Old retailers;
	
	public IdentifyRetailers (Retailers_Old retailers) {
		this.retailers = retailers;
	}
	
	public void identifyRetailers1 () {
		
		Iterator<Facility> iter_fac = this.retailers.getRetailers().values().iterator();
	
		while (iter_fac.hasNext()) {
			Facility f = iter_fac.next();
			if (f.getActivity("shop").getType().compareTo("shop")==0) {
			//	Retailers  =  
			}
		}
		
	}
	@Override
	public void run(Retailers_Old retailers) {
		// TODO Auto-generated method stub
		
	}

}
