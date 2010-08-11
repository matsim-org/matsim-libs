package playground.anhorni.locationchoice.preprocess.facilities.facilitiescreation.fromOldFacilities;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;

public class AddLeisureFacility2EveryLink {
	
	private final ScenarioImpl scenario = new ScenarioImpl();
	private final static Logger log = Logger.getLogger(AddLeisureFacility2EveryLink.class);
		
	public static void main(final String[] args) {
		String facilitiesFile = args[0];
		String networkFile = args[1];
		String outputFile = args[2];
				
		Gbl.startMeasurement();
		final AddLeisureFacility2EveryLink creator = new AddLeisureFacility2EveryLink();
		creator.run(facilitiesFile, networkFile, outputFile);
		Gbl.printElapsedTime();
	}
	
	public void run(String facilitiesFile, String networkFile, String outputFile) {
		log.info("reading facilities ...");	
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesFile);
		
		log.info("reading the network ...");
		new MatsimNetworkReader(this.scenario).readFile(networkFile);
		
		this.removeLeisureFacilities();
		this.addNewFacilities();
		
		this.writeFacilities(outputFile);
		log.info("finished");
	}
	
	private void removeLeisureFacilities() {
		// need a deep copy -> avoid java.util.ConcurrentModificationException
		Object [] oldFacilitiesArray = this.scenario.getActivityFacilities().getFacilities().values().toArray();
		
		for (int i = 0; i < oldFacilitiesArray.length; i++) {		
			ActivityFacilityImpl facility = (ActivityFacilityImpl)oldFacilitiesArray[i];
			if (!(facility.getActivityOptions().get("leisure") == null)) {
				this.scenario.getActivityFacilities().getFacilities().remove(facility.getId());
			}
		} 	
	}
		
	private void addNewFacilities() {
		log.info("adding new leisure facilities");
		
		int facilityNbr = 1;
		
		for (Link link : this.scenario.getNetwork().getLinks().values()) {
			
			Id id = new IdImpl(10000000 + facilityNbr);
			this.scenario.getActivityFacilities().createFacility(id, link.getCoord());
			
			ActivityFacilityImpl facility = this.scenario.getActivityFacilities().getFacilities().get(id);
			facility.createActivityOption("leisure");
			facility.getActivityOptions().get("leisure").setCapacity(1.0);
			facility.getActivityOptions().get("leisure").addOpeningTime(new OpeningTimeImpl(DayType.wk, 0.0 * 3600, 24.0 * 3600));
			
			facilityNbr++;
		}
		log.info("added " + facilityNbr + " leisure facilities");
	}
	
	private void writeFacilities(String outputFile) {
		log.info("Number of facilities: " + this.scenario.getActivityFacilities().getFacilities().size());
		new FacilitiesWriter(this.scenario.getActivityFacilities()).write(outputFile);
	}
}
