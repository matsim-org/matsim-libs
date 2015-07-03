package playground.dziemke.accessibility;

import java.util.ArrayList;
import java.util.Collection;
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
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author dziemke
 */
public class FacilityLocationAnalyzer {
	private static PointFeatureFactory pointFeatureFactory;
	
	// Parameters
	private static String[] attributeLabel = {"FacilityId","Type"};
	
	// Input file and output directory
	private static String facilitiesFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/facilities_landuse6.xml";
//	private static String facilitiesFile = "/Users/dominik/Workspace/matsimExamples/countries/za/nmbm/facilities/20121010/facilities.xml";
	private static String outputFileName = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/facilities_landuse6.shp";
	
	
	public static void main(String[] args) {
		Map <Id<ActivityFacility>, Coord> facilityCoords = new HashMap <Id<ActivityFacility>, Coord>();
		Map <Id<ActivityFacility>, String> facilityTypes = new HashMap <Id<ActivityFacility>, String>();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);	
		MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
		reader.readFile(facilitiesFile);

		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			facilityCoords.put(facility.getId(), facility.getCoord());
			
//			System.out.println("facility.getActivityOptions().toString() = " + facility.getActivityOptions().toString());
						
			for (ActivityOption activityOption : facility.getActivityOptions().values()) {
				facilityTypes.put(facility.getId(), activityOption.getType());
			}
		}

		initFeatureType(attributeLabel);
		Collection <SimpleFeature> features = createFeatures(facilityCoords, facilityTypes);
		ShapeFileWriter.writeGeometries(features, outputFileName);
	}
	
	
	private static void initFeatureType(String[] attributeLabel) {
		pointFeatureFactory = new PointFeatureFactory.Builder().
		setCrs(MGC.getCRS(TransformationFactory.DHDN_GK4)).
		setName("points").
		addAttribute(attributeLabel[0], String.class).
		addAttribute(attributeLabel[1], String.class).
		create();
	}	
	
	
	private static <T> Collection <SimpleFeature> createFeatures(Map<Id<T>,Coord> facilityCoords, Map<Id<T>,String> facilityTypes) {
		List <SimpleFeature> features = new ArrayList <SimpleFeature>();
		for (Id<?> id : facilityCoords.keySet()){
			Coord coord = facilityCoords.get(id);
			String type = facilityTypes.get(id);
			Object[] attributes = new Object[]{id, type};
			SimpleFeature feature = pointFeatureFactory.createPoint(coord, attributes, null);
			features.add(feature);
		}
		return features;
	}	
}