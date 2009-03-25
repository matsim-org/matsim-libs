package playground.jjoubert.CommercialTraffic;

import java.io.IOException;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

public class ReadStudyAreaShapeFile {
	
	public static MultiPolygon readStudyAreaPolygon( String shapeFileSource ) {
		
		FeatureSource fs = null;
		MultiPolygon mp = null;
		try {	
			fs = ShapeFileReader.readDataFile( shapeFileSource );
			for(Object o: fs.getFeatures() ){
				Geometry geo = ((Feature)o).getDefaultGeometry();
				if(geo instanceof MultiPolygon){
					mp = (MultiPolygon)geo;
				} else{
					System.out.println("The shapefile is not a multipolygon");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mp;
	}
}
