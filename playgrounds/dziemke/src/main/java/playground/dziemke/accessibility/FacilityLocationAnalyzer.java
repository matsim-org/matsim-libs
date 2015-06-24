package playground.dziemke.accessibility;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;

import playground.dziemke.analysis.location.PointShapeFileWriter;

/**
 * @author dziemke
 */
public class FacilityLocationAnalyzer {
	// Input file and output directory
	private static String facilitiesFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/facilities_amenities_modified.xml";
//	private static String facilitiesFile = "/Users/dominik/Workspace/matsimExamples/countries/za/nmbm/facilities/20121010/facilities.xml";
	private static String outputDirectory = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/";
	private static String outputFileName = "facilities_amenities.shp";
	private static String attributeName = "FacilityId";
	
	public static void main(String[] args) {
		Map <Id<ActivityFacility>, Coord> facilityCoords = new HashMap <Id<ActivityFacility>, Coord>();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);	
		MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
		reader.readFile(facilitiesFile);
		
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			facilityCoords.put(facility.getId(), facility.getCoord());
		}
		
		PointShapeFileWriter.writeShapeFilePoints(outputDirectory + outputFileName, facilityCoords, attributeName);
	}
}