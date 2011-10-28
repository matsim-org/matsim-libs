package playground.gregor.multidestpeds.helper;

import java.util.Collection;

import org.geotools.feature.Feature;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

public class LineStringsSnapper {

	private static final double SNAP = 0.1;

	public static void main(String[] args) {
		String in = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/tmp.shp";
		String out = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries_transformed_closed.shp";

		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(in);
		QuadTree<Coordinate> quad = new QuadTree<Coordinate>(reader.getBounds().getMinX(),reader.getBounds().getMinY(),reader.getBounds().getMaxX(),reader.getBounds().getMaxY());

		for (Feature ft : reader.getFeatureSet()) {
			MultiLineString ml = (MultiLineString) ft.getDefaultGeometry();
			for (int i = 0; i < ml.getNumGeometries(); i++) {
				LineString ls = (LineString) ml.getGeometryN(i);
				Point start = ls.getStartPoint();
				checkIt(start,quad);


				Point end = ls.getEndPoint();
				checkIt(end,quad);
			}
		}
		ShapeFileWriter.writeGeometries(reader.getFeatureSet(), out);
	}

	private static void checkIt(Point start, QuadTree<Coordinate> quad) {
		Collection<Coordinate> col = quad.get(start.getX(), start.getY(), SNAP);
		if (col.size() == 0) {
			quad.put(start.getX(), start.getY(), start.getCoordinate());
		} else if (col.size() == 1) {
			Coordinate other = col.iterator().next();
			start.getCoordinate().setCoordinate(other);
			System.out.println("SNAP, SNAP, SNAP...");
		} else {
			throw new RuntimeException("can not happen!");
		}

	}

}
