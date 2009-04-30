package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.locationchoice.assemblefacilities.BZReader;
import playground.anhorni.locationchoice.assemblefacilities.Hectare;

public class AnalyzeMZ {	
	private final static Logger log = Logger.getLogger(AnalyzeMZ.class);
	private String analyzeTrips = "false";
	private String analyzeActs = "false";
	
	
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
		log.info("Number of MZ trips: " + mzReader.getMzTrips().size());		
		
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
		List<MZTrip> mzTrips = groceryfilter.filterTrips(mzReader.getMzTrips());
				
		log.info("Reading BZ");
		BZReader bzReader = new BZReader();
		List<Hectare> hectares = bzReader.readBZGrocery("input/BZ/BZ01_UNT_P_DSVIEW.TXT");
		log.info("Number of hectares: " + hectares.size());
		
		log.info("Create hectare relations");
		CreateTripHectareRelation relationCreator = new CreateTripHectareRelation();
		List<MZTripHectare> relations = relationCreator.createRelations(mzTrips, hectares);
		log.info("Number of trip-hectare relations: " + relations.size());
		
		ActWriter writer = new ActWriter();
		String outpath = "output/valid/acts/";
		writer.write(relations, outpath);
		log.info("Finished analyzeActs ----------------------------------------------");
	}
	
	public void analyzeTrips(MZReader mzReader) {
				
		// Count acts by counting trips having wzweck2==Hinweg
		int countShopTripsHinweg = 0;
		int countLeisureTripsHinweg = 0;
		int countShopTrips = 0;
		int countLeisureTrips = 0;
		
		Iterator<MZTrip> mzTrips_it = mzReader.getMzTrips().iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			//Hinweg == 1
			if (mzTrip.getShopOrLeisure().equals("shop")) {
				if (mzTrip.getWzweck2().equals("1")) {
					countShopTripsHinweg++;
				}
				countShopTrips++;
			}
			if (mzTrip.getShopOrLeisure().equals("leisure")) {
				if (mzTrip.getWzweck2().equals("1")) {
					countLeisureTripsHinweg++;
				}
				countLeisureTrips++;
			}
		}
		
		String outpath = "./output/valid/trips/";
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath +"summary.txt");
			out.write("Number of trips : " + mzReader.getMzTrips().size() + "\n" +
					"Number of shop trips: " + countShopTrips + "\n" +  
					"Number of leisure trips: " + countLeisureTrips + "\n" +
					"Number of shop trips HINWEG: " + countShopTripsHinweg + "\n" +
					"Number of leisure trips HINWEG: " + countLeisureTripsHinweg);
			out.flush();
			out.close();			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		TreeMap<Id, PersonTrips>  personTrips = mzReader.getPersonTrips();
		log.info("Number of persons: " + personTrips.size());
		
		log.info("Writting ch trips file ...");
		
		
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
