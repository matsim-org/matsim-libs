package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import java.util.List;
import java.util.Vector;
import org.apache.log4j.Logger;

import playground.anhorni.locationchoice.assemblefacilities.BZReader;
import playground.anhorni.locationchoice.assemblefacilities.Hectare;
import playground.anhorni.locationchoice.cs.helper.MZTrip;



public class AnalyzeMZ {
	
	private final static Logger log = Logger.getLogger(AnalyzeMZ.class);

	private List<MZTrip> mzTrips = new Vector<MZTrip>();
	
	public static void main(String[] args) {
		AnalyzeMZ analyzer = new AnalyzeMZ();
		analyzer.run();
	}
	
	public void run() {
		MZReader mzReader = new MZReader();
		mzTrips = mzReader.read("input/cs/MZ2005_Wege.dat");
		log.info("Number of MZ trips: " + mzTrips.size());
		
		GroceryFilter groceryfilter = new GroceryFilter();
		mzTrips = groceryfilter.filterTrips(mzTrips);
		
		GeographicalFilter geographicalfilter = new GeographicalFilter();
		mzTrips = geographicalfilter.filterTrips(mzTrips);
		
		BZReader bzReader = new BZReader();
		List<Hectare> hectares = bzReader.readBZGrocery("input/facilities/BZ01_UNT_P_DSVIEW.TXT");
		
		CreateTripHectareRelation relationCreator = new CreateTripHectareRelation();
		List<MZTripHectare> relations = relationCreator.createRelations(mzTrips, hectares);
		
		TripWriter writer = new TripWriter();
		writer.write(relations);
	}
}
