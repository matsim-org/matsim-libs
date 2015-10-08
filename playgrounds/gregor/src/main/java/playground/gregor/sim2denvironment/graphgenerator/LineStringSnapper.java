package playground.gregor.sim2denvironment.graphgenerator;

import java.util.Collection;
import java.util.List;

import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class LineStringSnapper {
	private static final double SNAP = 0.1;
	public void run(List<LineString> ls, Envelope e) {
		QuadTree<Coordinate> quad = new QuadTree<Coordinate>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		for (LineString l : ls) {
			Point start = l.getStartPoint();
			checkIt(start,quad);
			Point end = l.getEndPoint();
			checkIt(end,quad);
		}
	}
	private void checkIt(Point start, QuadTree<Coordinate> quad) {
		Collection<Coordinate> col = quad.getDisk(start.getX(), start.getY(), SNAP);
		if (col.size() == 0) {
			quad.put(start.getX(), start.getY(), start.getCoordinate());
		} else if (col.size() == 1) {
			Coordinate other = col.iterator().next();
			start.getCoordinate().setCoordinate(new Coordinate(other));
			System.out.println("SNAP, SNAP, SNAP...");
		} else {
			throw new RuntimeException("can not happen!");
		}

	}
}
