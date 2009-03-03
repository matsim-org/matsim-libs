package playground.duncan.archive;

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.*;

public class Shp2Facilities {
	private static final Logger log = Logger.getLogger(Shp2Facilities.class);
	
	private static Collection<Feature> getPolygons(final FeatureSource n) {
		final Collection<Feature> polygons = new ArrayList<Feature>(); // not needed

		Facilities facilities = new Facilities("workplaces",false) ;
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
			
			Facility facility = facilities.createFacility(id, coord ) ;
			
			facility.createActivity( (String) feature.getAttribute("LU_CODE") ) ;
			facility.createActivity( (String) feature.getAttribute("LU_DESCRIP") ) ;

		}
		
		FacilitiesWriter facWriter = new FacilitiesWriter(facilities,"/home/nagel/landuse.xml.gz") ;
		facWriter.write();

		return polygons; // not needed
	}
	
	public static void main(final String [] args) {
		final String shpFile = "/Users/nagel/shared-svn/studies/north-america/ca/metro-vancouver/facilities/shp/landuse.shp";
		
		Gbl.createWorld();
		Gbl.createConfig(null);
		Collection<Feature> zones = null;
		try {
			zones = getPolygons(ShapeFileReader.readDataFile(shpFile));
		} catch (final Exception e) {
			e.printStackTrace();
		}
		
	}
	

}
