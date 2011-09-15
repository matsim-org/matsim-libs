package playground.gregor.multidestpeds.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class ShapeFileConverter {

	public static void main(String [] args) throws IOException {
		String input = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries.shp";
		String output = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries_transformt.shp";
		FeatureSource fs = ShapeFileReader.readDataFile(input);
		Iterator it = fs.getFeatures().iterator();
		List<Feature> fts = new ArrayList<Feature>();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Geometry g = ft.getDefaultGeometry();
			Coordinate[] coords = g.getCoordinates();
			for (Coordinate coord : coords) {
				Coordinate c = coord;
				Coordinate c2 = WGS86UTM33N2MathBuildingTransformation.transform(c);
				c.x = c2.x;
				c.y = c2.y;
				c.z = 0;
			}
			fts.add(ft);
		}

		ShapeFileWriter.writeGeometries(fts, output);

	}


}
