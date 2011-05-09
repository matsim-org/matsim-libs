package tutorial;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;

public class CreateFacilities {
	
	private final static Logger log = Logger.getLogger(CreateFacilities.class);
	private Scenario scenario;
	private String censusFile = "./input/census.txt";
	private String businessCensusFile = "./input/business_census.txt";

	public static void main(String[] args) {
		CreateFacilities facilitiesCreator = new CreateFacilities();
		facilitiesCreator.init();
		facilitiesCreator.run();
		facilitiesCreator.write();
		log.info("Creation finished #################################");
	}
	
	private void init() {
		/*
		 * Create the scenario
		 */
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);	
	}
	
	private void run() {
		/*
		 * Read the business census for work, shop, leisure and education facilities
		 */
		int startIndex = this.readBusinessCensus();
		
		/*
		 * Read the census for home facilities. Other sources such as official dwelling directories could be used as well.
		 * Usually some aggregation should be done. In this example we simply add every home location as a facility.
		 */
		this.readCensus(startIndex);
		
	}
	
	private int readBusinessCensus() {
		int cnt = 0;
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.businessCensusFile));
			String line = bufferedReader.readLine(); //skip header
			
			// id = 0
			int index_xCoord = 1;
			int index_yCoord = 2;
			int index_types = 3;
			
			
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");
				
				Coord coord = new CoordImpl(Double.parseDouble(parts[index_xCoord]),
						Double.parseDouble(parts[index_yCoord]));
				
				ActivityFacilityImpl facility = 
					(ActivityFacilityImpl)((ScenarioImpl)this.scenario).getActivityFacilities().createFacility(new IdImpl(cnt), coord);
				
				String types [] = parts[index_types].split(",");
 				for (int i = 0; i < types.length; i++) {
 					this.addActivityOption(facility, types[i]);
				}
				cnt++;
			}
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
		return cnt;
	}
	
	private void readCensus(int startIndex) {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.censusFile));
			String line = bufferedReader.readLine(); //skip header
			
			int index_xHomeCoord = 10;
			int index_yHomeCoord = 11;
			
			int cnt = 0;
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");
				
				Coord homeCoord = new CoordImpl(Double.parseDouble(parts[index_xHomeCoord]),
						Double.parseDouble(parts[index_yHomeCoord]));
				
				ActivityFacility facility = ((ScenarioImpl)this.scenario).getActivityFacilities().createFacility(new IdImpl(startIndex + cnt), homeCoord);
				addActivityOption(facility, "home");
				cnt++;
			}
			
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void addActivityOption(ActivityFacility facility, String type) {
		((ActivityFacilityImpl) facility).createActivityOption(type);
		
		/*
		 * [[ 1 ]] Specify the opening hours here. An example is given for the activity work.
		 */
		ActivityOptionImpl actOption = (ActivityOptionImpl)facility.getActivityOptions().get(type);
		OpeningTimeImpl opentime;
		if (type.equals("shop")) {
			opentime = new OpeningTimeImpl(DayType.wkday, 8.0 * 3600.0, 19.0 * 3600); //[[ 1 ]] opentime = null;
		}
		else if (type.equals("leisure") || type.equals("education")) {
			opentime = new OpeningTimeImpl(DayType.wk, 8.0 * 3600.0, 19.0 * 3600); //[[ 1 ]] opentime = null;
		}
		else if (type.equals("work")) {
			opentime = new OpeningTimeImpl(DayType.wkday, 8.0 * 3600.0, 19.0 * 3600); //[[ 1 ]] opentime = null;
		}
		// home
		else {
			opentime = new OpeningTimeImpl(DayType.wk, 0.0 * 3600.0, 24.0 * 3600);
		}
		actOption.addOpeningTime(opentime);	
	}
		
	public void write() {
		new FacilitiesWriter(((ScenarioImpl) this.scenario).getActivityFacilities()).write("./output/facilities.xml.gz");
	}
}
