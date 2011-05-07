package tutorial;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
	
	private Scenario scenario;
	private String censusFile = "./input/census.txt";
	private String businessCensusFile = "./input/business_census.txt";

	public static void main(String[] args) {
		CreateFacilities facilitiesCreator = new CreateFacilities();
		facilitiesCreator.init();
		facilitiesCreator.run();
		facilitiesCreator.write();
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
		 * Read the entreprise census for work, shop, leisure and education facilities
		 */
		int startIndex = this.readEntrepriseCensus();
		
		/*
		 * Read the census for home facilities. Other sources such as official dwelling directories could be used as well.
		 * Usually some aggregation should be done. In this example we simply add every home location as a facility.
		 */
		this.readCensus(startIndex);
		
	}
	
	private int readEntrepriseCensus() {
		int cnt = 0;
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.businessCensusFile));
			String line = bufferedReader.readLine(); //skip header
			
			int index_type = 0;
			int index_xHomeCoord = 0;
			int index_yHomeCoord = 0;
			
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");
				
				Coord coord = new CoordImpl(Double.parseDouble(parts[index_xHomeCoord]),
						Double.parseDouble(parts[index_yHomeCoord]));
				
				this.addFacility(coord, new IdImpl(cnt), parts[index_type]);
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
			
			int index_xHomeCoord = 0;
			int index_yHomeCoord = 0;
			
			int cnt = 0;
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split("\t");
				
				Coord homeCoord = new CoordImpl(Double.parseDouble(parts[index_xHomeCoord]),
						Double.parseDouble(parts[index_yHomeCoord]));
				
				this.addFacility(homeCoord, new IdImpl(startIndex + cnt), "home");
				cnt++;
			}
			
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void addFacility(Coord coord, Id id, String type) {
		ActivityFacilityImpl facility = (ActivityFacilityImpl)((ScenarioImpl)this.scenario).getActivityFacilities().createFacility(id, coord);	
		facility.createActivityOption(type);
		
		/*
		 * [[ 1 ]] Specify the opening hours here. An example is given for the activity work.
		 */
		ActivityOptionImpl actOption = (ActivityOptionImpl)facility.getActivityOptions().get(type);
		OpeningTimeImpl opentime;
		if (type.equals("shop")) {
			opentime = null;
		}
		else if (type.equals("leisure") || type.equals("education")) {
			opentime = null;
		}
		// work
		else {
			opentime = new OpeningTimeImpl(DayType.wk, 8.0 * 3600.0, 19.0 * 3600);
		}
		actOption.addOpeningTime(opentime);
	}
	
	public void write() {
		new FacilitiesWriter(((ScenarioImpl) this.scenario).getActivityFacilities()).write("./output/facilities.xml");
	}
}
