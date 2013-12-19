package playground.dziemke.utils;

import java.util.Collection;
import java.util.Map;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class LORShapeReader {
	
	public static void readShape(String shapeFileLors, Map<Integer, String> lors) {
		Collection<SimpleFeature> allLors = ShapeFileReader.getAllFeatures(shapeFileLors);
	
		for (SimpleFeature lor : allLors) {
			Integer lorschluessel = Integer.parseInt((String) lor.getAttribute("SCHLUESSEL"));
			String name = (String) lor.getAttribute("LOR");
			lors.put(lorschluessel, name);
		}
	}

}
