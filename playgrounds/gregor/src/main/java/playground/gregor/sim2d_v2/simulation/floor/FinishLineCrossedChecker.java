package playground.gregor.sim2d_v2.simulation.floor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;



import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Triangle;

public class FinishLineCrossedChecker {

	private final Scenario sc;

	private HashMap<Id, FinishLines> finishLines;
	private HashMap<Id, LineString> perpendicularLines;

	private GeometryFactory geofac;

	private List<Geometry> geos;


	private static final double COS_LEFT = Math.cos(Math.PI / 2);
	private static final double SIN_LEFT = Math.sin(Math.PI / 2);
	private static final double COS_RIGHT = Math.cos(-Math.PI / 2);
	private static final double SIN_RIGHT = Math.sin(-Math.PI / 2);

	private final double earlySwitchOffset = 0.25;

	public FinishLineCrossedChecker(Scenario sc) {
		this.sc = sc;
	}

	public void init() {

		initGeometries();

		this.finishLines = new HashMap<Id, FinishLines>();
		this.perpendicularLines = new HashMap<Id, LineString>();
		this.geofac = new GeometryFactory();
		for (Link link : this.sc.getNetwork().getLinks().values()) {
			FinishLines lines = new FinishLines();
			for (Link next : link.getToNode().getOutLinks().values()) {
				LineString ls = null;
				if (next.getToNode() == link.getFromNode()) {
					ls = getPerpendicularLine(link);
					this.perpendicularLines.put(link.getId(), ls);
				} else{
					ls = getBisectorialLine(link,next);
				}
				lines.finishLines.put(next.getId(), ls);
			}
			this.finishLines.put(link.getId(), lines);
		}
	}

	private void initGeometries() {
		this.geos = new ArrayList<Geometry>();
		for (Feature ft : this.sc.getScenarioElement(ShapeFileReader.class).getFeatureSet()) {
			this.geos.add(ft.getDefaultGeometry());
		}

	}

	private LineString getBisectorialLine(Link link, Link next) {

		Coordinate a = MGC.coord2Coordinate(link.getFromNode().getCoord());
		Coordinate b = MGC.coord2Coordinate(link.getToNode().getCoord());
		Coordinate c = MGC.coord2Coordinate(next.getToNode().getCoord());
		Coordinate d = Triangle.angleBisector(a, b, c);

		double lengthAb = a.distance(b);

		double abx = b.x - a.x;
		double aby = b.y - a.y;

		double nbx = a.x + abx/lengthAb * (lengthAb - this.earlySwitchOffset);
		double nby = a.y + aby/lengthAb * (lengthAb - this.earlySwitchOffset);

		Coordinate nb = new Coordinate(nbx,nby);

		double lengthBd = b.distance(d);
		double dx = 30 *(d.x - b.x) / lengthBd;
		double dy = 30 * (d.y - b.y)/ lengthBd;

		Coordinate c1 = new Coordinate(nbx + dx, nby + dy);
		Coordinate c2 = new Coordinate(nbx - dx, nby - dy);




		LineString bisec = this.geofac.createLineString(new Coordinate[]{c1,c2});

		List<Coordinate> intersects = getIntersections(bisec);

		double minCc1 = Double.POSITIVE_INFINITY;
		double minCc2 = Double.POSITIVE_INFINITY;

		Coordinate cc1 = c1;
		Coordinate cc2 = c2;

		for (Coordinate intersection : intersects) {
			if (c1.distance(intersection) < c2.distance(intersection)) {
				if (minCc1 > intersection.distance(nb)) {
					minCc1 = intersection.distance(nb);
					cc1 = intersection;
				}
			} else {
				if (minCc2 > intersection.distance(nb)) {
					minCc2 = intersection.distance(nb);
					cc2 = intersection;
				}
			}
		}

		LineString ret = this.geofac.createLineString(new Coordinate[]{cc1,cc2});

		return ret;
	}

	private List<Coordinate> getIntersections(LineString bisec) {
		List<Coordinate> ret = new ArrayList<Coordinate>();
		for (Geometry geo : this.geos) {
			Geometry itrs = geo.intersection(bisec);
			if (itrs.isEmpty()) {
				continue;
			}

			if (itrs instanceof Point) {
				ret.add(itrs.getCoordinate());
			} else if (itrs instanceof MultiPoint){
				for (int i = 0; i < itrs.getNumGeometries(); i++) {
					ret.add(itrs.getGeometryN(i).getCoordinate());
				}
			} else  {
				throw new RuntimeException("Geometry type" + itrs.getGeometryType() + " is not supported here!");
			}
		}
		return ret;
	}

	private LineString getPerpendicularLine(Link link) {
		Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());
		Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
		Coordinate c = new Coordinate(from.x - to.x, from.y - to.y);
		// length of finish line is 30 m// TODO does this make sense?
		double scale = 30 / Math.sqrt(Math.pow(c.x, 2) + Math.pow(c.y, 2));
		c.x *= scale;
		c.y *= scale;
		Coordinate c1 = new Coordinate(COS_LEFT * c.x + SIN_LEFT * c.y, -SIN_LEFT * c.x + COS_LEFT * c.y);
		c1.x += to.x;
		c1.y += to.y;
		Coordinate c2 = new Coordinate(COS_RIGHT * c.x + SIN_RIGHT * c.y, -SIN_RIGHT * c.x + COS_RIGHT * c.y);
		c2.x += to.x;
		c2.y += to.y;
		LineString ls = this.geofac.createLineString(new Coordinate[] { c1, c2 });


		return ls;
	}

	public boolean crossesFinishLine(Id currentLinkId, Id nextLinkId,
			Coordinate oldPos, Coordinate newPos) {
		LineString ls;
		if (nextLinkId == null) {
			ls = this.perpendicularLines.get(currentLinkId);
		} else {
			ls = this.finishLines.get(currentLinkId).finishLines.get(nextLinkId);
		}
		LineString trajectory = this.geofac.createLineString(new Coordinate[]{oldPos,newPos});
		return trajectory.crosses(ls);
	}

	private static final class FinishLines {
		public Map<Id,LineString> finishLines = new HashMap<Id,LineString>();
	}

}
