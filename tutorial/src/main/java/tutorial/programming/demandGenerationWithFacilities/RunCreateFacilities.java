package tutorial.programming.demandGenerationWithFacilities;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class RunCreateFacilities {
	
	private final static Logger log = Logger.getLogger(RunCreateFacilities.class);
	private Scenario scenario;
	private static final String censusFile = "examples/tutorial/programming/demandGenerationWithFacilities/census.txt";
	private static final String businessCensusFile = "examples/tutorial/programming/demandGenerationWithFacilities/business_census.txt";

	public static void main(String[] args) {
		RunCreateFacilities facilitiesCreator = new RunCreateFacilities();
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
			BufferedReader bufferedReader = new BufferedReader(new FileReader(businessCensusFile));
			bufferedReader.readLine(); //skip header
			
			// id = 0
			int index_xCoord = 1;
			int index_yCoord = 2;
			int index_types = 3;
			
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");

				Coord coord = new Coord(Double.parseDouble(parts[index_xCoord]), Double.parseDouble(parts[index_yCoord]));
				
				ActivityFacilityImpl facility = (ActivityFacilityImpl)this.scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create(cnt, ActivityFacility.class), coord);
				this.scenario.getActivityFacilities().addActivityFacility(facility);
				
				String types [] = parts[index_types].split(",");
				for (String type : types) {
					this.addActivityOption(facility, type);
				}
				cnt++;
			}
			bufferedReader.close();
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
		return cnt;
	}
	
	private void readCensus(int startIndex) {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(censusFile));
			bufferedReader.readLine(); //skip header
			
			int index_xHomeCoord = 10;
			int index_yHomeCoord = 11;
			
			int cnt = 0;
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");

				Coord homeCoord = new Coord(Double.parseDouble(parts[index_xHomeCoord]), Double.parseDouble(parts[index_yHomeCoord]));
				
				ActivityFacility facility = this.scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create(startIndex + cnt, ActivityFacility.class), homeCoord);
				addActivityOption(facility, "home");
				scenario.getActivityFacilities().addActivityFacility(facility);
				cnt++;
			}
			bufferedReader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void addActivityOption(ActivityFacility facility, String type) {
		((ActivityFacilityImpl) facility).createAndAddActivityOption(type);
		
		/*
		 * [[ 1 ]] Specify the opening hours here for shopping and leisure. An example is given for the activities work and home.
		 */
		ActivityOptionImpl actOption = (ActivityOptionImpl)facility.getActivityOptions().get(type);
		if (type.equals("work")) {
			actOption.addOpeningTime(new OpeningTimeImpl(8.0 * 3600.0, 19.0 * 3600)); //[[ 1 ]] opentime = null;
		}

	}
		
	public void write() {
		new FacilitiesWriter(this.scenario.getActivityFacilities()).write("./output/facilities.xml");
	}
}
