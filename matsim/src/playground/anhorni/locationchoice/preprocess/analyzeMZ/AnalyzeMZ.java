package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import java.util.List;
import java.util.Vector;

import playground.anhorni.locationchoice.cs.helper.MZTrip;
import playground.anhorni.locationchoice.facilities.BZReader;
import playground.anhorni.locationchoice.facilities.Hectare;

public class AnalyzeMZ {

	private List<MZTrip> mzTrips = new Vector<MZTrip>();
	
	public static void main(String[] args) {
		AnalyzeMZ analyzer = new AnalyzeMZ();
		analyzer.run();
	}
	
	public void run() {
		MZReader mzReader = new MZReader();
		mzTrips = mzReader.read("input/MZ/MZ2005_Wege.dat");
		
		GroceryFilter groceryfilter = new GroceryFilter();
		groceryfilter.filterTrips(mzTrips);
		
		BZReader bzReader = new BZReader();
		List<Hectare> hectares = bzReader.readBZGrocery("input/facilities/BZ01_UNT_P_DSVIEW.TXT");
	}
}
