package playground.anhorni.locationchoice.valid;

import java.util.Iterator;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import playground.anhorni.locationchoice.preprocess.analyzeMZ.GeographicalFilter;
import playground.anhorni.locationchoice.preprocess.analyzeMZ.MZReader;
import playground.anhorni.locationchoice.preprocess.analyzeMZ.PersonTrips;



public class ValidationMZ {
	
	private final static Logger log = Logger.getLogger(ValidationMZ.class);
	
	public static void main(String[] args) {
		ValidationMZ analyzer = new ValidationMZ();
		analyzer.run();
	}
	
	public void run() {
		log.info("Reading MZ ...");
		MZReader mzReader = new MZReader();
		mzReader.read("input/cs/MZ2005_Wege.dat");
		
		TreeMap<Id, PersonTrips>  personTrips = mzReader.getPersonTrips();
		log.info("Number of persons: " + personTrips.size());
		
		GeographicalFilter geographicalFilter = new GeographicalFilter();
		personTrips = geographicalFilter.filterPersons(personTrips);		
		log.info("Number of persons after geographical filtering: " + personTrips.size());		
		
		log.info("Writting trips file ...");
		
		TreeMap<Id, PersonTrips>  personTripsFiltered = new TreeMap<Id, PersonTrips>();
		Iterator<PersonTrips> personTrips_it = personTrips.values().iterator();
		while (personTrips_it.hasNext()) {
			PersonTrips pt = personTrips_it.next();
			if (pt.intersectZH()) {
				personTripsFiltered.put(pt.getPersonId(), pt);
			}
		}
		log.info("Number of persons after ZH filtering: " + personTripsFiltered.size());
		
		TripWriter writer = new TripWriter();
		writer.write(personTripsFiltered);
		log.info("Finished");
	}
}
