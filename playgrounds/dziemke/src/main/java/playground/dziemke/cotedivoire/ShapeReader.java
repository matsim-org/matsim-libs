package playground.dziemke.cotedivoire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

public class ShapeReader {
	
	public static List <Region> read (String filename){

		List <Region> regions = new ArrayList <Region>();
		
		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features;
		
		features = reader.readFileAndInitialize(filename);
		int i=1;
		for (SimpleFeature feature : features) {
			String name = (String) feature.getAttribute("NAME_1");
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			
			Region region = new Region(i, name, geometry);
			regions.add(region);
			
			System.out.println(i + ".te Region: " + name);
			i++;
		}	
		
		return regions;
	}
}