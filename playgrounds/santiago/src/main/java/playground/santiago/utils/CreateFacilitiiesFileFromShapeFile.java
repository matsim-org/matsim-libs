package playground.santiago.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;


/**
 * @author dziemke
 */
public class CreateFacilitiiesFileFromShapeFile {
	static String shapeFileName = "../../../../Downloads/Servicio_Impuestos_Internos/Manzanas_STGO.shp"; // TODO adapt paths
	static String attributeCaption = "CMN_MZ"; // TODO adapt to relevant column
	static String facilitiesOutputFile = "../../../../Downloads/facilites.xml"; // TODO adapt paths
	
	public static void main(String[] args) {
		Map<String, SimpleFeature> blocks = collectFeatures();		
		ActivityFacilities activityFacilities = createFacilities(blocks);
		writeFacilitiesFile(activityFacilities);
	}

	
	private static Map<String, SimpleFeature> collectFeatures() {
		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize(shapeFileName);
		Map <String, SimpleFeature> blocks = new HashMap<>();
		for (SimpleFeature feature : features) {
			blocks.put((String) feature.getAttribute(attributeCaption), feature);
		}
		return blocks;
	}
	
	
	private static ActivityFacilities createFacilities(Map<String, SimpleFeature> blocks) {
		ActivityFacilities activityFacilities = FacilitiesUtils.createActivityFacilities("Homes"); // adapt name
		ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
		for (String blockId : blocks.keySet()) {		
			Id<ActivityFacility> id = Id.create(blockId, ActivityFacility.class);
//			Geometry geometry = (Geometry) blocks.get(blockId); // TODO use it; see below
			Coord coord = CoordUtils.createCoord(120., 130.); // TODO use meaningful coordinates here; use geometry of feature
			ActivityFacility activityFacility = activityFacilitiesFactory.createActivityFacility(id, coord);
			activityFacilities.addActivityFacility(activityFacility);
		}
		return activityFacilities;
	}
	
	
	private static void writeFacilitiesFile(ActivityFacilities activityFacilities) {
		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(activityFacilities);
		facilitiesWriter.write(facilitiesOutputFile);
	}
}