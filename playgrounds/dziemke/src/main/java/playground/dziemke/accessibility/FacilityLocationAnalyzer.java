package playground.dziemke.accessibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author dziemke
 */
public class FacilityLocationAnalyzer {
	// Input file and output directory
	private static String inputOutputDirectory = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/07/";
	
//	private static String facilitiesFile = "../../matsimExamples/countries/za/nmbm/facilities/20121010/facilities.xml";
	private static String facilitiesFile = inputOutputDirectory + "/facilities.xml";
//	private static String facilitiesFile = "../../projects/accessibility_berlin/osm/berlin/08/facilities_buildings.xml";
//	private static String facilitiesFile = "../../projects/accessibility_berlin/osm/berlin/facilities_amenities_modified.xml";
	
//	private static String outputFileName = "../../accessibility-sa/data/facilities.shp";
	private static String outputFileName = inputOutputDirectory + "facilities.shp";
//	private static String outputFileName = "../../shared-svn/projects/accessibility_berlin/osm/berlin/08/facilities_buildings.shp";
//	private static String outputFileName = "../../projects/accessibility_berlin/osm/berlin/facilities_amenities.shp";
	
	// Parameters
//	private static String[] attributeLabel = {"FacilityId", "Type", "Name"};
//	static String crs = TransformationFactory.DHDN_GK4;
	static String crs = "EPSG:31468"; // = DHDN GK4
//	static String crs = TransformationFactory.WGS84_SA_Albers;
	
	
	private static PointFeatureFactory pointFeatureFactory;
	
	
	public static void main(String[] args) {
		Map <Id<ActivityFacility>, Coord> facilityCoords = new HashMap <Id<ActivityFacility>, Coord>();
//		Map <Id<ActivityFacility>, String> facilityTypes = new HashMap <Id<ActivityFacility>, String>();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);	
		MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
		reader.readFile(facilitiesFile);

//		initFeatureType(attributeLabel);
		initFeatureType();
		
		List <SimpleFeature> features = new ArrayList <SimpleFeature>();

		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			facilityCoords.put(facility.getId(), facility.getCoord());
			
			String facilityDescription = ((ActivityFacilityImpl) facility).getDesc();
			
//			System.out.println("facility.getActivityOptions().toString() = " + facility.getActivityOptions().toString());
						
			for (ActivityOption activityOption : facility.getActivityOptions().values()) {
				Object[] attributes = new Object[]{facility.getId(), activityOption.getType(), facilityDescription};
				SimpleFeature feature = pointFeatureFactory.createPoint( facility.getCoord(), attributes, null ) ;
				features.add(feature);
			}
		}

		ShapeFileWriter.writeGeometries(features, outputFileName);
	}
	

//	private static void initFeatureType(String[] attributeLabel) {
	private static void initFeatureType() {
		pointFeatureFactory = new PointFeatureFactory.Builder().
		setCrs(MGC.getCRS(crs)).
//		setName("points").
//		addAttribute(attributeLabel[0], String.class).
//		addAttribute(attributeLabel[1], String.class).
//		addAttribute(attributeLabel[2], String.class).
		addAttribute("FacilityId", String.class).
		addAttribute("Type", String.class).
		addAttribute("Name", String.class).
		create();
	}	
}