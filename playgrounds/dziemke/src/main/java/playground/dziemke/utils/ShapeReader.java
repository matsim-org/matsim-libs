package playground.dziemke.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

public class ShapeReader {
	
	public static Map <Integer, Geometry> read (String filename, String attributeCaption){

		Map <Integer, Geometry> zoneGeometries = new HashMap <Integer, Geometry>();
		
		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features;
		
		features = reader.readFileAndInitialize(filename);
		for (SimpleFeature feature : features) {
			zoneGeometries.put(Integer.parseInt((String) feature.getAttribute(attributeCaption)),(Geometry) feature.getDefaultGeometry());
		}	
		
		return zoneGeometries;
	}
}