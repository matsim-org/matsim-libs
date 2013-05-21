package playground.dziemke.lor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

public class ShapeReader {
	
	// private Map<Integer, Zone> zones = new HashMap<Integer, Zone>();
	
	public static List <Lor> read (String filename){
	// public static List <Lor> read (String filename){

		List <Lor> lors = new ArrayList <Lor>();
		
		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features;
		
		features = reader.readFileAndInitialize(filename);
		for (SimpleFeature feature : features) {
			String id = (String) feature.getAttribute("SCHLUESSEL");
			String name = (String) feature.getAttribute("LOR");
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			
			Lor lor = new Lor(id, name, geometry);
			lors.add(lor);
			
			// System.out.println("Schluessel: " + id + "; Name: "+ name);
		}	
		
		return lors;
	}
}