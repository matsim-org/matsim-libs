package playground.wrashid.tryouts.geo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.spatialschema.geometry.primitive.PrimitiveFactory;


import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File file = new File("P:/Daten/GIS_Daten/ArcView/PLZ_Layers/PLZ_Reg_region.shp");

		try {
		  Map connect = new HashMap();
		  connect.put("url", file.toURL());

		  DataStore dataStore = DataStoreFinder.getDataStore(connect);
		  String[] typeNames = dataStore.getTypeNames();
		  String typeName = typeNames[0];

		  System.out.println("Reading content " + typeName);

		  FeatureSource featureSource = dataStore.getFeatureSource(typeName);
		  FeatureCollection collection = featureSource.getFeatures();
		  FeatureIterator iterator = collection.features();

		 // GeometryFactory.createPointFromInternalCoord(coord, exemplar)
		  
		 // WKTParser parser = new WKTParser( new Geome DefaultGeographicCRS.WGS84 );
		 // Point point = (Point) parser.parse("POINT( 48.44 -123.37)");
		  Point p = MGC.coord2Point(new CoordImpl(744120.000135, 234919.999845));

		  try {
		    while (iterator.hasNext()) {
		      Feature feature = iterator.next();
		      Geometry sourceGeometry = feature.getDefaultGeometry();
		     
		     
		     
		     if (!sourceGeometry.disjoint(p)){
		    	 System.out.println(sourceGeometry.getCoordinate());
		     }
		     
		    }
		  } finally {
		    iterator.close();
		  }

		} catch (Throwable e) {}


	}

}
