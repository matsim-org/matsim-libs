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
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
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
//	private static String inputOutputDirectory = "../../../shared-svn/projects/maxess/data/nairobi/kodi/schools/secondary/";
	private static String inputOutputDirectory = "../../../shared-svn/projects/maxess/data/nairobi/facilities/2017-04-25_nairobi_central_and_kibera/";
	
	private static String facilitiesFile = inputOutputDirectory + "2017-04-25_facilities.xml";
	private static String outputFileName = inputOutputDirectory + "2017-04-25_facilities.shp";
	
	// Parameters
//	private static String[] attributeLabel = {"FacilityId", "Type", "Name"};
//	static String crs = TransformationFactory.DHDN_GK4;
//	static String crs = "EPSG:31468"; // = DHDN GK4, for Berlin
	static String crs = "EPSG:21037"; // = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
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