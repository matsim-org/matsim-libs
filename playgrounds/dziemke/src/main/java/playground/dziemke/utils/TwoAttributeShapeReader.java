package playground.dziemke.utils;

import java.util.Collection;
import java.util.Map;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class TwoAttributeShapeReader {
	
	public static void readShape(String shapeFile, Map<Integer, String> featureMap, String attributeKey, String attributeName) {
		Collection<SimpleFeature> collectionOfFeatures = ShapeFileReader.getAllFeatures(shapeFile);
	
		for (SimpleFeature feature : collectionOfFeatures) {
			Integer key = Integer.parseInt((String) feature.getAttribute(attributeKey));
			String name = (String) feature.getAttribute(attributeName);
			featureMap.put(key, name);
		}
	}

}
