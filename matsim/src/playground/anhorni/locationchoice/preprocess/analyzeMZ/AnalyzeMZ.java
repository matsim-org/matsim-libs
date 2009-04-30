package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.gbl.Gbl;

import playground.anhorni.locationchoice.assemblefacilities.BZReader;
import playground.anhorni.locationchoice.assemblefacilities.Hectare;
import playground.anhorni.locationchoice.cs.helper.MZTrip;

public class AnalyzeMZ {	
	private final static Logger log = Logger.getLogger(AnalyzeMZ.class);
	private String analyzeTrips = "false";
	private String analyzeActs = "false";
	private List<MZTrip> mzTrips = new Vector<MZTrip>();
	
	public static void main(String[] args) {	
		AnalyzeMZ analyzer = new AnalyzeMZ();
		analyzer.run();
	}
	
	public void run() {
		String inputFile = "./input/trb/valid/input.txt";
		this.readInputFile(inputFile);
		
		log.info("Reading MZ ...");
		MZReader mzReader = new MZReader();
		mzReader.read("input/MZ/MZ2005_Wege.dat");
		log.info("Number of MZ trips: " + mzTrips.size());		
		
		if (this.analyzeTrips.equals("true")) {
			this.analyzeTrips(mzReader);
		}
		if (this.analyzeActs.equals("true")) {
			this.analyzeActs(mzReader);
		}		
	}
		
	public void analyzeActs(MZReader mzReader) {
		log.info("Starting analyze acts");
		GroceryFilter groceryfilter = new GroceryFilter();
		mzTrips = groceryfilter.filterTrips(mzTrips);
		
		GeographicalFilter geographicalfilter = new GeographicalFilter();
		mzTrips = geographicalfilter.filterTrips(mzTrips);
		
		log.info("Reading BZ");
		BZReader bzReader = new BZReader();
		List<Hectare> hectares = bzReader.readBZGrocery("input/BZ/BZ01_UNT_P_DSVIEW.TXT");
		
		log.info("Create hectare relations");
		CreateTripHectareRelation relationCreator = new CreateTripHectareRelation();
		List<MZTripHectare> relations = relationCreator.createRelations(mzTrips, hectares);
		
		ActWriter writer = new ActWriter();
		String outpath = "output/valid/acts/";
		writer.write(relations, outpath);
		log.info("Finished analyzeActs ----------------------------------------------");
	}
	
	public void analyzeTrips(MZReader mzReader) {	
		TreeMap<Id, PersonTrips>  personTrips = mzReader.getPersonTrips();
		log.info("Number of persons: " + personTrips.size());
		
		GeographicalFilter geographicalFilter = new GeographicalFilter();
		personTrips = geographicalFilter.filterPersons(personTrips);		
		log.info("Number of persons after plausiblity check: " + personTrips.size());		
		
		log.info("Writting ch trips file ...");
		String outpath = "./output/valid/trips/";
		
		TripWriter writer = new TripWriter();
		writer.write(personTrips, "ch", outpath);
		
		
		TreeMap<Id, PersonTrips>  personTripsFiltered = new TreeMap<Id, PersonTrips>();
		Iterator<PersonTrips> personTrips_it = personTrips.values().iterator();
		while (personTrips_it.hasNext()) {
			PersonTrips pt = personTrips_it.next();
			if (pt.intersectZH()) {
				personTripsFiltered.put(pt.getPersonId(), pt);
			}
		}
		log.info("Number of persons after ZH filtering: " + personTripsFiltered.size());
		
		log.info("Writting zh trips file ...");
		writer = new TripWriter();
		writer.write(personTripsFiltered, "zh", outpath);
		log.info("Finished analyzeTrips ----------------------------------------------");
	}
	
	private void readInputFile(final String inputFile) {
		try {
			FileReader fileReader = new FileReader(inputFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			this.analyzeTrips = bufferedReader.readLine();
			this.analyzeActs = bufferedReader.readLine();

			bufferedReader.close();
			fileReader.close();

		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
}
