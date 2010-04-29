package playground.duncan.archive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Shp2Facilities {
	private static final Logger log = Logger.getLogger(Shp2Facilities.class);
	
	private static Collection<Feature> getPolygons(final FeatureSource n, final ScenarioImpl scenario) {
		final Collection<Feature> polygons = new ArrayList<Feature>(); // not needed

		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		facilities.setName("workplaces");
		long cnt = 0 ;
		
		FeatureIterator it = null;
		try {
			it = n.getFeatures().features();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		while (it.hasNext()) {
			final Feature feature = it.next();
			
			String str ;
			
			str = "AREA" ;
			System.out.println( str + ": " + feature.getAttribute(str) ) ; 

			str = "LU_CODE" ;
			System.out.println( str + ": " + feature.getAttribute(str) ) ; 

			str = "LU_DESCRIP" ;
			System.out.println( str + ": " + feature.getAttribute(str) ) ; 

			final MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			if (multiPolygon.getNumGeometries() > 1) {
				log.warn("MultiPolygons with more then 1 Geometry ignored!");
//				continue;
			}
			final Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			Point center = polygon.getCentroid();
			Coord coord = new CoordImpl ( center.getX() , center.getY() ) ;
			
			Id id = new IdImpl( cnt ) ; cnt++ ;
			
			ActivityFacilityImpl facility = facilities.createFacility(id, coord ) ;
			
			facility.createActivityOption( (String) feature.getAttribute("LU_CODE") ) ;
			facility.createActivityOption( (String) feature.getAttribute("LU_DESCRIP") ) ;

		}
		
		new FacilitiesWriter(facilities).write("/home/nagel/landuse.xml.gz") ;

		return polygons; // not needed
	}
	
	public static void main(final String [] args) {
		final String shpFile = "/Users/nagel/shared-svn/studies/north-america/ca/metro-vancouver/facilities/shp/landuse.shp";
		
		ScenarioImpl scenario = new ScenarioImpl();
		Collection<Feature> zones = null;
		try {
			zones = getPolygons(ShapeFileReader.readDataFile(shpFile), scenario);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		
	}
	

}
