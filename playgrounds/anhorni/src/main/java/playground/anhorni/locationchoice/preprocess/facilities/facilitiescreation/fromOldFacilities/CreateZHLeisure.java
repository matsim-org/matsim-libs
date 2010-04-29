package playground.anhorni.locationchoice.preprocess.facilities.facilitiescreation.fromOldFacilities;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;


public class CreateZHLeisure {
	
	private final ScenarioImpl scenario = new ScenarioImpl();
	private ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
	private final static Logger log = Logger.getLogger(CreateZHLeisure.class);
	
	public static void main(final String[] args) {
		Gbl.startMeasurement();
		final CreateZHLeisure creator = new CreateZHLeisure();
		creator.run();
		Gbl.printElapsedTime();
	}
	
	public void run() {
		log.info("reading facilities ...");
				
		new FacilitiesReaderMatsimV1(scenario).readFile("input/facilities/facilities.xml.gz");
		this.createNewFacilities();
		this.writeFacilities();
		log.info("finished");
	}
	
	private void createNewFacilities() {
				
		// need a deep copy -> avoid java.util.ConcurrentModificationException
		Object [] oldFacilitiesArray = this.facilities.getFacilities().values().toArray();
		for (int i = 0; i < oldFacilitiesArray.length; i++) {		
			ActivityFacilityImpl facility = (ActivityFacilityImpl)oldFacilitiesArray[i];
			if (!(facility.getActivityOptions().get("leisure") == null)) {
				this.facilities.getFacilities().remove(facility.getId());
			}
		}	
		this.addNewFacilities();
	}
	
	private void addNewFacilities() {
		log.info("adding new leisure facilities");
		// check that:
		// ivtch bellevue node 2531:
		// x="683508.5" y="246832.9063" 
		double xCenterNode = 683508.5;
		double yCenterNode = 246832.9063;
		CoordImpl centerNode = new CoordImpl(xCenterNode, yCenterNode);
		double xRectOrig = xCenterNode - 30000;
		double yRectOrig = yCenterNode - 30000;
		
		int cnt = 0;
		for (int i = 0; i < 60000; i += 200) {
			double x= xRectOrig + i;
			for (int j = 0; j < 60000; j += 200) {
				double y = yRectOrig + j;
				CoordImpl coord = new CoordImpl(x, y);
				if (coord.calcDistance(centerNode) <= 30000) {
					IdImpl id = new IdImpl(3000000 + cnt);
					cnt++;
					this.facilities.createFacility(id, coord);				
					ActivityFacilityImpl facility = this.facilities.getFacilities().get(id);
					facility.createActivityOption("leisure");
					facility.getActivityOptions().get("leisure").setCapacity(1.0);
					facility.getActivityOptions().get("leisure").addOpeningTime(new OpeningTimeImpl(DayType.wk, 9.0 * 3600, 24.0 * 3600));
				}
			}
		}
		log.info("added " + cnt + " new leisure facilities");
	}
	
	private void writeFacilities() {
		log.info("Number of facilities: " + this.facilities.getFacilities().size());
		new FacilitiesWriter(this.facilities).write("output/facilitiesgeneration/facilities.xml.gz");
	}
}
