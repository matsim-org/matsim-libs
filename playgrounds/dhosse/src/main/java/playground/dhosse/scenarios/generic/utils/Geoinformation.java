package playground.dhosse.scenarios.generic.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.gap.Global;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * Class for reading and storing geoinformation.
 * 
 * @author dhosse
 *
 */
public class Geoinformation {
	
	private static Map<String, Geometry> geometries = new HashMap<String, Geometry>();
	
	public static void readGeodataFromShapefile(String filename, Set<String> ids){
		
		Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(filename);
		
		for(SimpleFeature feature : features){
			
			String kennzahl = Long.toString((Long)feature.getAttribute("GEM_KENNZ"));
			
			if(ids.contains(kennzahl)){
				
				geometries.put(kennzahl, (Geometry)feature.getDefaultGeometry());
				
			}
			
		}
		
	}
	
	public static void readGeodataFromShapefileWithFilter(String filename, Set<String> filterIds){
		
		Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(filename);
		
		for(SimpleFeature feature : features){
			
			String kennzahl = Long.toString((Long)feature.getAttribute("GEM_KENNZ"));
			
			for(String id : filterIds){
				
				if(kennzahl.startsWith(id)){
					
					geometries.put(kennzahl, (Geometry)feature.getDefaultGeometry());
					break;
					
				}
				
			}
			
		}
		
	}
	
	public static Map<String, Geometry> getGeometries(){
		
		return geometries;
		
	}
	
	public static Coord shoot(Geometry geometry){
		
		Point point = null;
		double x, y;
		
		do{
			
			x = geometry.getEnvelopeInternal().getMinX() + Global.random.nextDouble() * (geometry.getEnvelopeInternal().getMaxX() - geometry.getEnvelopeInternal().getMinX());
	  	    y = geometry.getEnvelopeInternal().getMinY() + Global.random.nextDouble() * (geometry.getEnvelopeInternal().getMaxY() - geometry.getEnvelopeInternal().getMinY());
	  	    point = MGC.xy2Point(x, y);
			
		}while(!geometry.contains(point));
		
		return MGC.point2Coord(point);
		
	}

}
