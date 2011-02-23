package playground.gregor.gis.coordiantemapping;

import java.io.IOException;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Point;

import playground.gregor.MY_STATIC_STUFF;

public class CoordianteMapping {
	
	public static void main (String [] args) throws IOException {
		String infile = MY_STATIC_STUFF.SVN_ROOT + "/shared-svn/projects/120multiDestPeds/floor_plan/refpoints.shp";
		FeatureSource fs = ShapeFileReader.readDataFile(infile);
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Point p = (Point) ft.getDefaultGeometry();
			System.out.println(ft.getAttribute("ID") + "," + p.getX() + "," + p.getY());
		}
	}

}
