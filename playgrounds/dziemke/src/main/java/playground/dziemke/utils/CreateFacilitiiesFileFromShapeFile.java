package playground.dziemke.utils;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;


/**
 * @author dziemke
 */
public class CreateFacilitiiesFileFromShapeFile {
	static String shapeFileName = "../../../shared-svn/projects/maxess/data/nairobi/nairobi_LU_2010/nairobi_LU.shp";
	static String attributeCaption = "LANDUSE";
	static String facilitiesOutputFile = "../../../shared-svn/projects/maxess/data/nairobi/nairobi_LU_2010/facilites.xml";
	
	public static void main(String[] args) {
		Collection<SimpleFeature> blocks = collectFeatures();		
		ActivityFacilities activityFacilities = createFacilities(blocks);
		writeFacilitiesFile(activityFacilities);
	}

	
	private static Collection<SimpleFeature> collectFeatures() {
		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize(shapeFileName);
//		Map <String, SimpleFeature> blocks = new HashMap<>();
//		for (SimpleFeature feature : features) {
//			blocks.put((String) feature.getAttribute(attributeCaption), feature);
//		}
		return features;
	}
	
	
	private static ActivityFacilities createFacilities(Collection<SimpleFeature> blocks) {
		ActivityFacilities activityFacilities = FacilitiesUtils.createActivityFacilities("Homes"); // adapt name
		ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
		int i = 1;
		for (SimpleFeature feature : blocks) {		
//			Id<ActivityFacility> id = Id.create((String) feature.getAttribute("OBJECTID"), ActivityFacility.class);
			Id<ActivityFacility> id = Id.create(i , ActivityFacility.class);
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			Point point = geometry.getCentroid();
			Coord coord = CoordUtils.createCoord(point.getX(), point.getY());
			ActivityFacility activityFacility = activityFacilitiesFactory.createActivityFacility(id, coord);
			String landUseType = (String) feature.getAttribute("LANDUSE");
			ActivityOption activityOption = activityFacilitiesFactory.createActivityOption(landUseType);
			activityFacility.addActivityOption(activityOption);
			activityFacilities.addActivityFacility(activityFacility);
			i++;
		}
		return activityFacilities;
	}
	
	
	private static void writeFacilitiesFile(ActivityFacilities activityFacilities) {
		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(activityFacilities);
		facilitiesWriter.write(facilitiesOutputFile);
	}
}