package playground.gregor.multiPedCrowds;

import java.io.IOException;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class AsciiFromShp {



	public static void main(String [] args) throws IOException {
		String out = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries_transformed_closed.shp";
		FeatureSource fs = ShapeFileReader.readDataFile(out);
		Iterator it = fs.getFeatures().iterator();

		System.out.println("#linenr,typ,x,y");
		int i = 0;
		while (it.hasNext() ) {
			Feature ft = (Feature) it.next();
			LineString ls = (LineString)((MultiLineString)ft.getDefaultGeometry()).getGeometryN(0);
			for (int j = 0; j < ls.getNumPoints(); j++) {
				System.out.println(i +"," + ft.getAttribute("lines") + "," + ls.getCoordinateN(j).x + "," + ls.getCoordinateN(j).y);

			}
			i++;

		}
	}
}
