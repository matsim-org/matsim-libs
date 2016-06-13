package playground.santiago.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import com.vividsolutions.jts.geom.Geometry;



public class ShapeReader {
	
	public static Map <String, Geometry> read (String filename, String attributeCaption){

		Map <String, Geometry> zoneGeometries = new HashMap <String, Geometry>();
		
		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features;
		
		features = reader.readFileAndInitialize(filename);
		for (SimpleFeature feature : features) {
			zoneGeometries.put((String) feature.getAttribute(attributeCaption),(Geometry) feature.getDefaultGeometry());
		}	
		
		return zoneGeometries;
	}
}