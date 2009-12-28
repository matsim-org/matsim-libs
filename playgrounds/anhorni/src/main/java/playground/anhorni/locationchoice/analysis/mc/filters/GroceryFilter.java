package playground.anhorni.locationchoice.analysis.mc.filters;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.locationchoice.analysis.mc.MZTrip;


public class GroceryFilter {
			
	public List<MZTrip> filterTrips(List<MZTrip> mzTrips) {
		
		Vector<MZTrip> filteredTrips = new Vector<MZTrip>(); 
		int numberOfShop = 0;
		
		Iterator<MZTrip> mztrips_it = mzTrips.iterator();
		while (mztrips_it.hasNext()) {
			MZTrip mzTrip = mztrips_it.next();
			
			if (mzTrip.getPurpose().equals("shop") && !mzTrip.getPurposeCode().equals("-99")) {
				numberOfShop++;
			}
			
			if (mzTrip.getPurpose().equals("shop") && mzTrip.getPurposeCode().equals("1")) {
				filteredTrips.add(mzTrip);
			}	
		}
		this.writeGrocerySummary(numberOfShop, filteredTrips.size());
		return filteredTrips;
	}
		
	private void writeGrocerySummary(int numberOfShoppingActs, int numberOfGroceryShoppingActs) {
		try {
			BufferedWriter out = IOUtils.getBufferedWriter("output/analyzeMz/acts/grocery_acts.txt");
			DecimalFormat formatter = new DecimalFormat("0.00");
			
			out.write("Total number of shopping activities: " + numberOfShoppingActs +"\n");
			out.write("Total number of grocery shopping activities: " + numberOfGroceryShoppingActs +"\n");
			out.write("Percent of grocery shopping activities: " + 
					formatter.format(100 * numberOfGroceryShoppingActs/numberOfShoppingActs) +"\n");
			out.flush();
			out.close();			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
}
