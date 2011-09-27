package playground.gregor.sim2d_v2.experimental;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class SkeletonExtractor {

	private final GeometryFactory geofac = new GeometryFactory();

	public Skeleton extractSkeleton(GeometryCollection voronoiDiagram, Geometry boundary) {

		Envelope e = getEnvelope(voronoiDiagram);

		Skeleton ret = new Skeleton(e);
		int num = voronoiDiagram.getNumGeometries();
		for (int i = 0; i < num; i ++) {
			Polygon geo = (Polygon) voronoiDiagram.getGeometryN(i);
			LineString shell = geo.getExteriorRing();
			for (int j =1; j < shell.getNumPoints(); j++) {
				LineString tmp = this.geofac.createLineString(new Coordinate[]{new Coordinate(shell.getCoordinateN(j-1)),new Coordinate(shell.getCoordinateN(j))});
				if (boundary.disjoint(tmp)) {
					createLink(ret,tmp);
				}
			}
		}

		return ret;
	}

	private Envelope getEnvelope(GeometryCollection voronoiDiagram) {
		Envelope e = new Envelope(voronoiDiagram.getCoordinate());
		for (Coordinate c : voronoiDiagram.getCoordinates()) {
			e.expandToInclude(c);
		}
		return e;
	}

	private void createLink(Skeleton ret, LineString tmp) {
		Point from = tmp.getStartPoint();
		Point to = tmp.getEndPoint();
		SkeletonNode fromNode = ret.getOrCreateNode(from);
		SkeletonNode toNode = ret.getOrCreateNode(to);

		for (SkeletonLink l : fromNode.getLinkedLinks()) {
			if (l.getToNode().equals(toNode) || l.getFromNode().equals(toNode)) {
				return;
			}
		}
		ret.createAndAddLink(fromNode,toNode);
	}
}
