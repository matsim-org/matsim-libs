package playground.anhorni.locationchoice.analysis.mc;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.anhorni.locationchoice.analysis.mc.filters.MZTripValidator;
import playground.anhorni.locationchoice.analysis.mc.io.MZReader;

public class MZ {
	
	private List<MZTrip> mzTripsAllTypes = new Vector<MZTrip>();
	private final static Logger log = Logger.getLogger(MZ.class);
	
	private TripsPerTypeHandling tripsPerTypeHandler = new TripsPerTypeHandling();
	
	private Persons persons = new Persons();	
	
	private String infile = "input/MZ/MZ2005_Wegeinland.dat";
	private MZReader reader = new MZReader(this.mzTripsAllTypes);
			
	public MZ() {		
	}
		
	public void readFile() {
		this.reader.read(this.infile);		
		
		MZTripValidator validator = new MZTripValidator();
		this.mzTripsAllTypes = validator.filterTrips(this.mzTripsAllTypes);
		
		this.personTrips(validator.getRemovedTripsFromPersons());
			
		log.info("Creating shop, leisure and work distributions");
		this.tripsPerTypeHandler.createDistributions(this.mzTripsAllTypes);
	}
		
	private void personTrips(HashSet<Id> removedTripsFromPersons) {
		
		Iterator<MZTrip> mzTrips_it = mzTripsAllTypes.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			persons.addTrip(mzTrip.getPersonId(), mzTrip);
		}
		this.cleanTouchedPersons(removedTripsFromPersons);
		log.info("Number of untouched persons: " + persons.getNumberOfPersons());
	}
	
	private void cleanTouchedPersons(HashSet<Id> removedTripsFromPersons) {
		
		log.info("Cleaning persons ...");
		TreeMap<Id, PersonTripActs> cleanedPersons = (TreeMap<Id, PersonTripActs>)persons.getPersons().clone();
		
		Iterator<Id> person_it = persons.getPersons().keySet().iterator();
		while (person_it.hasNext()) {
			Id personId = person_it.next();
			
			if (removedTripsFromPersons.contains(personId)) {
				cleanedPersons.remove(personId);
				removedTripsFromPersons.remove(personId);
			}
		}
		this.persons.setPersons(cleanedPersons);
	}
		
	public List<MZTrip> getMzTripsAllTypes() {
		return mzTripsAllTypes;
	}
	public void setMzTripsAllTypes(List<MZTrip> mzTripsAllTypes) {
		this.mzTripsAllTypes = mzTripsAllTypes;
	}

	public MZReader getReader() {
		return reader;
	}
	public void setReader(MZReader reader) {
		this.reader = reader;
	}

	public TreeMap<String, TripMeasureDistribution> getShopTrips() {
		return this.tripsPerTypeHandler.getShopTrips();
	}

	public TreeMap<String, TripMeasureDistribution> getLeisureTrips() {
		return tripsPerTypeHandler.getLeisureTrips();
	}
	
	public TreeMap<String, TripMeasureDistribution> getWorkTrips() {
		return tripsPerTypeHandler.getWorkTrips();
	}
	
	public TreeMap<String, TripMeasureDistribution> getEducationTrips() {
		return tripsPerTypeHandler.getEducationTrips();
	}

	public Persons getPersons() {
		return persons;
	}

	public void setPersons(Persons persons) {
		this.persons = persons;
	}	
}
