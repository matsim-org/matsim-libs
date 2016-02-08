package org.tit.matsim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTimeImpl;

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
			
			
			ActivityFacilities facilities = this.scenario.getActivityFacilities();
			ActivityFacilitiesFactory factory = facilities.getFactory();
			
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");

				Coord coord = new Coord(Double.parseDouble(parts[index_xCoord]), Double.parseDouble(parts[index_yCoord]));
				
				ActivityFacility facility = factory.createActivityFacility(Id.create(cnt, ActivityFacility.class), coord);
				facilities.addActivityFacility(facility);
				
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
			
			ActivityFacilities facilities = this.scenario.getActivityFacilities();
			ActivityFacilitiesFactory factory = facilities.getFactory();
			
			int cnt = 0;
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");

				Coord homeCoord = new Coord(Double.parseDouble(parts[index_xHomeCoord]), Double.parseDouble(parts[index_yHomeCoord]));
				
				ActivityFacility facility = factory.createActivityFacility(Id.create(startIndex + cnt, ActivityFacility.class), homeCoord);
				facilities.addActivityFacility(facility);
				addActivityOption(facility, "home");
				cnt++;
			}
			
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void addActivityOption(ActivityFacility facility, String type) {
		((ActivityFacilityImpl) facility).createAndAddActivityOption(type);
		
		/*
		 * [[ 1 ]] Specify the opening hours here for shopping and leisure. An example is given for the activities work and home.
		 */
		ActivityOptionImpl actOption = (ActivityOptionImpl)facility.getActivityOptions().get(type);
		OpeningTimeImpl opentime;
		if (type.equals("shop")) {
			opentime = new OpeningTimeImpl(10 * 3600.0, 20 * 3600); ;
		}
		else if (type.equals("leisure") || type.equals("education")) {
			opentime = new OpeningTimeImpl(8.0 * 3600.0, 19.0 * 3600); ;
		}
		else if (type.equals("work")) {
			opentime = new OpeningTimeImpl(8.0 * 3600.0, 19.0 * 3600); //[[ 1 ]] opentime = null;
		}
		// home
		else {
			opentime = new OpeningTimeImpl(0.0 * 3600.0, 24.0 * 3600);
		}
		actOption.addOpeningTime(opentime);	
	}
		
	public void write() {
		new FacilitiesWriter(((MutableScenario) this.scenario).getActivityFacilities()).write("./output/facilities.xml.gz");
	}
}
