package playground.gregor.ctsim.simulation.physics;

import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;
import com.vividsolutions.jts.geom.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vehicles.Vehicle;
import playground.gregor.ctsim.run.CTRunner;
import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.events.debug.LineEvent;

import java.util.*;

public class CTLink implements CTNetworkEntity {


	static final double WIDTH = 1;
	private static final Logger log = Logger.getLogger(CTLink.class);
	private static final double EPSILON = 0.00001;
	private final CTNetwork network;
	private final CTNode dsNode;
	private final CTNode usNode;
	private final List<CTCell> cells = new ArrayList<>();
	private Link dsLink;
	private Link usLink;
	private EventsManager em;
	private CTLinkCell dsJumpOn;
	private CTLinkCell usJumpOn;

	public CTLink(Link l, Link rev, EventsManager em, CTNetwork ctNetwork, CTNode from, CTNode to) {
		this.dsLink = l;
		this.dsNode = to;
		this.usNode = from;
		this.usLink = rev;
		this.em = em;
		this.network = ctNetwork;

	}

	@Override
	public void init() {

		//this requires a planar coordinate system
		double dx = (this.dsLink.getToNode().getCoord().getX() - this.dsLink.getFromNode().getCoord().getX()); // this.dsLink.getLength();
		double dy = (this.dsLink.getToNode().getCoord().getY() - this.dsLink.getFromNode().getCoord().getY()); // this.dsLink.getLength();
		double projLength = Math.sqrt(dx * dx + dy * dy);
		if (projLength == 0) {
			dx = 1;
			dy = 1;
			projLength = 1;
		}
		dx /= projLength;
		dy /= projLength;
		double length = this.dsLink.getLength();
		if (Math.abs(dy) < EPSILON) { //fixes a numerical issue
			dy = 0;
		}

		debugBound(dx, dy);

		double width = Math.max(this.dsLink.getCapacity() / 1.33, 2);


		Coordinate[] bounds = new Coordinate[5];
		bounds[0] = new Coordinate(this.dsLink.getFromNode().getCoord().getX() - dy * width / 2,
				this.dsLink.getFromNode().getCoord().getY() + dx * width / 2);
		bounds[1] = new Coordinate(this.dsLink.getFromNode().getCoord().getX() + dx * length - dy * width / 2,
				this.dsLink.getFromNode().getCoord().getY() + dy * length + dx * width / 2);
		bounds[2] = new Coordinate(this.dsLink.getFromNode().getCoord().getX() + dx * length + dy * width / 2,
				this.dsLink.getFromNode().getCoord().getY() + dy * length - dx * width / 2);
		bounds[3] = new Coordinate(this.dsLink.getFromNode().getCoord().getX() + dy * width / 2,
				this.dsLink.getFromNode().getCoord().getY() - dx * width / 2);
		bounds[4] = bounds[0];
		GeometryFactory geofac = new GeometryFactory();
		LinearRing lr = geofac.createLinearRing(bounds);

		Polygon p = (Polygon) geofac.createPolygon(lr, null).buffer(0.1);
		List<ProtoCell> cells = computeProtoCells(dx, dy);

		Geometry fromBnd = geofac.createLineString(new Coordinate[]{bounds[3], bounds[4]}).buffer(0.1);
		Geometry toBnd = geofac.createLineString(new Coordinate[]{bounds[1], bounds[2]}).buffer(0.1);

		Map<ProtoCell, CTCell> cellsMap = new HashMap<>();
		Map<ProtoCell, Geometry> geoMap = new HashMap<>();
		for (ProtoCell pt : cells) {
			CTCell c = new CTLinkCell(pt.x, pt.y, this.network, this, WIDTH);
			c.setArea(1.5 * Math.sqrt(3) * WIDTH * WIDTH);

			cellsMap.put(pt, c);
			Coordinate[] coords = new Coordinate[pt.edges.size() * 2];
			int idx = 0;
			for (GraphEdge ge : pt.edges) {
				coords[idx++] = new Coordinate(ge.x1, ge.y1);
				coords[idx++] = new Coordinate(ge.x2, ge.y2);
			}
			MultiPoint mp = geofac.createMultiPoint(coords);
			Geometry ch = mp.convexHull();
			geoMap.put(pt, ch);
		}
		Envelope e = new Envelope(bounds[0]);
		e.expandToInclude(bounds[1]);
		e.expandToInclude(bounds[2]);
		e.expandToInclude(bounds[3]);
		QuadTree<CTCell> qt = new QuadTree<>(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());

		for (ProtoCell pt : cells) {
			CTCell cell = cellsMap.get(pt);
			Geometry ch = geoMap.get(pt);
			if (p.covers(ch)) {
				this.cells.add(cell);
				qt.put(cell.getX(), cell.getY(), cell);
				for (GraphEdge ge : pt.edges) {
					debugGe(ge);
					ProtoCell protoNeighbor = pt.nb.get(ge);
					Geometry nCh = geoMap.get(protoNeighbor);
					CTCell neighbor = null;
					if (p.covers(nCh)) {
						neighbor = cellsMap.get(protoNeighbor);
					}
					else {
						if (fromBnd.intersects(nCh)) {
							neighbor = this.usNode.getCTCell();
							CTCellFace nFace = new CTCellFace(ge.x1, ge.y1, ge.x2, ge.y2, cell, -Math.PI / 2);
							neighbor.addFace(nFace);
						}
						else {
							if (toBnd.intersects(nCh)) {
								neighbor = this.dsNode.getCTCell();
								CTCellFace nFace = new CTCellFace(ge.x1, ge.y1, ge.x2, ge.y2, cell, Math.PI / 2);
								neighbor.addFace(nFace);
							}
						}
					}

					if (neighbor != null) {
						double dir = getAngle(cell.getX(), cell.getY(), (ge.x1 + ge.x2) / 2, (ge.y1 + ge.y2) / 2, cell.getX() + dy, cell.getY() - dx);
						CTCellFace face = new CTCellFace(ge.x1, ge.y1, ge.x2, ge.y2, neighbor, dir);
						cell.addFace(face);
					}
				}
			}
		}


		//identify cells
		Set<CTCell> dsJumpOns = new HashSet<>();
		Set<CTCell> usJumpOns = new HashSet<>();
		for (double incr = 0; incr <= width; incr += WIDTH / 2.) {
			double dsX = bounds[1].x + dy * incr - WIDTH * dx / 2.;
			double dsY = bounds[1].y - dx * incr - WIDTH * dy / 2.;
			double usX = bounds[0].x + dy * incr + WIDTH * dx / 2.;
			double usY = bounds[0].y - dx * incr + WIDTH * dy / 2.;
//			debugEntrances(dsX, usX, dsY, usY);
			CTCell dsJumpOn = qt.getClosest(dsX, dsY);

			dsJumpOns.add(dsJumpOn);
			CTCell usJumpOn = qt.getClosest(usX, usY);
			usJumpOns.add(usJumpOn);


		}


		//create pseudo cells
		this.dsJumpOn = new CTLinkCell(Double.NaN, Double.NaN, this.network, this, width / WIDTH);
		double dir = Math.PI / 2.;
		for (CTCell ctCell : dsJumpOns) {
			CTCellFace face = new CTCellFace(Double.NaN, Double.NaN, Double.NaN, Double.NaN, ctCell, dir);
			this.dsJumpOn.addFace(face);
			ctCell.addNeighbor(this.dsJumpOn);
		}
		this.usJumpOn = new CTLinkCell(0, 0, this.network, this, width / WIDTH);
		dir = -Math.PI / 2.;
		for (CTCell ctCell : usJumpOns) {
			CTCellFace face = new CTCellFace(Double.NaN, Double.NaN, Double.NaN, Double.NaN, ctCell, dir);
			this.usJumpOn.addFace(face);
			ctCell.addNeighbor(this.usJumpOn);
		}

		//append cells


//        for (CTCell c : this.cells) {
//            c.debug(this.em);
//        }

		if (CTRunner.DEBUG) {
			for (CTCell c : dsJumpOns) {
				c.g = 255;
				c.debug(this.em);
			}

			for (CTCell c : usJumpOns) {
				c.r = 255;
				c.g = 0;
				c.b = 0;
				c.debug(this.em);
			}
		}
	}


	private double getAngle(double frX, double frY, double toX1, double toY1, double toX2, double toY2) {

		final double l1 = Math.sqrt(3) / 4 * WIDTH;
		double cosAlpha = ((toX1 - frX) * (toX2 - frX) + (toY1 - frY) * (toY2 - frY)) / l1;
		double alpha = Math.acos(cosAlpha);
		if (CGAL.isLeftOfLine(toX1, toY1, frX, frY, toX2, toY2) < 0) {
			alpha -= Math.PI;
			alpha = -(Math.PI + alpha);
		}
//        debugAngle(alpha,frX,frY,toX1,toY1);
		return alpha;
	}

	private List<ProtoCell> computeProtoCells(double dx, double dy) {
		List<ProtoCell> cells = new ArrayList<>();

		double w = this.dsLink.getCapacity() / 1.33 + WIDTH * 2;
		double l = this.dsLink.getLength() + WIDTH * Math.sqrt(3) / 2;


		Voronoi v = new Voronoi(0.0001);


		List<Double> xl = new ArrayList<>();
		List<Double> yl = new ArrayList<>();
		boolean even = true;
		for (double yIncr = -WIDTH * Math.sqrt(3) / 4; yIncr < l; yIncr += WIDTH * Math.sqrt(3) / 4) {


			double xIncr = 0;
			if (even) {
				xIncr += WIDTH * 0.75;
			}
			even = !even;
			int idx = 0;
			for (; xIncr < w; xIncr += WIDTH * 1.5) {
				double y = this.dsLink.getFromNode().getCoord().getY() + dx * w / 2 - dx * WIDTH * Math.sqrt(3) / 8 + dy * yIncr - dx * xIncr;
				double x = this.dsLink.getFromNode().getCoord().getX() - dy * w / 2 + dy * WIDTH * Math.sqrt(3) / 8 + dx * yIncr + dy * xIncr;
				//				double x = x0 + xIncr*ldx;
				yl.add(y);
				xl.add(x);
				ProtoCell cell = new ProtoCell(x, y, idx++);
				cells.add(cell);

			}

		}
		double[] xa = new double[xl.size()];
		double[] ya = new double[xl.size()];
		for (int i = 0; i < xl.size(); i++) {
			xa[i] = xl.get(i);
			ya[i] = yl.get(i);
		}

		double length = this.dsLink.getLength();
		double y0 = this.dsLink.getFromNode().getCoord().getY() + dx * w / 2;
		double x0 = this.dsLink.getFromNode().getCoord().getX() - dy * w / 2;
		double y2 = this.dsLink.getFromNode().getCoord().getY() - dx * w / 2;
		double x2 = this.dsLink.getFromNode().getCoord().getX() + dy * w / 2;
		double y1 = this.dsLink.getFromNode().getCoord().getY() + dy * length - dx * w / 2;
		double x1 = this.dsLink.getFromNode().getCoord().getX() + dx * length + dy * w / 2;
		double y3 = this.dsLink.getFromNode().getCoord().getY() + dy * length + dx * w / 2;
		double x3 = this.dsLink.getFromNode().getCoord().getX() + dx * length - dy * w / 2;

		double minX = x1 < x0 ? x1 : x0;
		minX = minX < x2 ? minX : x2;
		minX = minX < x3 ? minX : x3;
		double maxX = x1 > x0 ? x1 : x0;
		maxX = maxX > x2 ? maxX : x2;
		maxX = maxX > x3 ? maxX : x3;
		double minY = y1 < y0 ? y1 : y0;
		minY = minY < y2 ? minY : y2;
		minY = minY < y3 ? minY : y3;
		double maxY = y1 > y0 ? y1 : y0;
		maxY = maxY > y2 ? maxY : y2;
		maxY = maxY > y3 ? maxY : y3;
		List<GraphEdge> edges = v.generateVoronoi(xa, ya, minX - WIDTH, maxX + WIDTH, minY - WIDTH, maxY + WIDTH);
		for (GraphEdge ge : edges) {

			ProtoCell c0 = cells.get(ge.site1);
			c0.edges.add(ge);
			ProtoCell c1 = cells.get(ge.site2);
			c1.edges.add(ge);
			c0.nb.put(ge, c1);
			c1.nb.put(ge, c0);

		}
		return cells;
	}

	private void debugGe(GraphEdge ge) {
		if (!CTRunner.DEBUG) {
			return;
		}
		LineSegment s = new LineSegment();
		s.x0 = ge.x1;
		s.x1 = ge.x2;
		s.y0 = ge.y1;
		s.y1 = ge.y2;
		LineEvent le = new LineEvent(0, s, true, 128, 128, 128, 255, 10, 0.1, 0.2);
		em.processEvent(le);
	}

	private void debugBound(double dx, double dy) {
		if (!CTRunner.DEBUG) {
			return;
		}


		double length = this.dsLink.getLength();

		{
			LineSegment s = new LineSegment();
			s.x0 = this.dsLink.getFromNode().getCoord().getX() - dy * this.dsLink.getCapacity() / 2.66;
			s.x1 = this.dsLink.getFromNode().getCoord().getX() + dx * length - dy * this.dsLink.getCapacity() / 2.66;
			s.y0 = this.dsLink.getFromNode().getCoord().getY() + dx * this.dsLink.getCapacity() / 2.66;
			s.y1 = this.dsLink.getFromNode().getCoord().getY() + dy * length + dx * this.dsLink.getCapacity() / 2.66;
			LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
			em.processEvent(le);
		}
		{
			LineSegment s = new LineSegment();
			s.x0 = this.dsLink.getFromNode().getCoord().getX() - dy * this.dsLink.getCapacity() / 2.66;
			s.x1 = this.dsLink.getFromNode().getCoord().getX() + dy * this.dsLink.getCapacity() / 2.66;
			s.y0 = this.dsLink.getFromNode().getCoord().getY() + dx * this.dsLink.getCapacity() / 2.66;
			s.y1 = this.dsLink.getFromNode().getCoord().getY() - dx * this.dsLink.getCapacity() / 2.66;
			LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
			em.processEvent(le);
		}
		{
			LineSegment s = new LineSegment();
			s.x0 = this.dsLink.getFromNode().getCoord().getX() + dy * this.dsLink.getCapacity() / 2.66;
			s.x1 = this.dsLink.getFromNode().getCoord().getX() + dx * length + dy * this.dsLink.getCapacity() / 2.66;
			s.y0 = this.dsLink.getFromNode().getCoord().getY() - dx * this.dsLink.getCapacity() / 2.66;
			s.y1 = this.dsLink.getFromNode().getCoord().getY() + dy * length - dx * this.dsLink.getCapacity() / 2.66;
			LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
			em.processEvent(le);
		}
		{
			LineSegment s = new LineSegment();
			s.x0 = this.dsLink.getFromNode().getCoord().getX() + dx * length - dy * this.dsLink.getCapacity() / 2.66;
			s.x1 = this.dsLink.getFromNode().getCoord().getX() + dx * length + dy * this.dsLink.getCapacity() / 2.66;
			s.y0 = this.dsLink.getFromNode().getCoord().getY() + dy * length + dx * this.dsLink.getCapacity() / 2.66;
			s.y1 = this.dsLink.getFromNode().getCoord().getY() + dy * length - dx * this.dsLink.getCapacity() / 2.66;
			LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
			em.processEvent(le);
		}

	}

	private void debugEntrances(double dsX, double usX, double dsY, double usY) {
		{
			LineSegment s = new LineSegment();
			s.x0 = dsX;
			s.y0 = dsY;
			s.x1 = dsX + 0.1;
			s.y1 = dsY + 0.1;
			LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
			em.processEvent(le);
		}
		{
			LineSegment s = new LineSegment();
			s.x0 = usX;
			s.y0 = usY;
			s.x1 = usX + 0.1;
			s.y1 = usY + 0.1;
			LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
			em.processEvent(le);
		}
	}

	public Link getDsLink() {
		return this.dsLink;
	}

	public Link getUsLink() {
		return this.usLink;
	}

	private void debugAngle(double alpha, double frX, double frY, double toX1, double toY1) {
		if (!CTRunner.DEBUG) {
			return;
		}
		int sect = (int) Math.round(10 * (Math.PI / alpha));
		LineSegment ls = new LineSegment();
		ls.x0 = frX;
		ls.y0 = frY;
		ls.x1 = toX1;
		ls.y1 = toY1;
		if (sect == 60) {
			LineEvent le = new LineEvent(0, ls, true, 192, 0, 0, 255, 0);
			em.processEvent(le);
		}
		else {
			if (sect == 20) {
				LineEvent le = new LineEvent(0, ls, true, 192, 192, 0, 255, 0);
				em.processEvent(le);
			}
			else {
				if (sect == 12) {
					LineEvent le = new LineEvent(0, ls, true, 0, 192, 0, 255, 0);
					em.processEvent(le);
				}
				else {
					if (sect == -12) {
						LineEvent le = new LineEvent(0, ls, true, 0, 192, 192, 255, 0);
						em.processEvent(le);
					}
					else {
						if (sect == -20) {
							LineEvent le = new LineEvent(0, ls, true, 0, 0, 192, 255, 0);
							em.processEvent(le);
						}
						else {
							if (sect == -60) {
								LineEvent le = new LineEvent(0, ls, true, 192, 0, 192, 255, 0);
								em.processEvent(le);
							}
						}
					}
				}
			}
		}

	}

	public List<CTCell> getCells() {
		return cells;
	}

//    public void letAgentDepart(CTVehicle veh, double now) {
//
//    }

	public void letAgentDepart(MobsimDriverAgent agent, CTLink link, double now) {

		CTCell cell;
		if (agent.getCurrentLinkId() == this.dsLink.getId()) {
			cell = this.dsJumpOn;
		}
		else {
			if (agent.getCurrentLinkId() == this.usLink.getId()) {
				cell = this.usJumpOn;
			}
			else {
				throw new RuntimeException("agent tries to depart on wrong link");
			}
		}
		CTPed p = new CTPed(cell, agent);
		Wait2LinkEvent e = new Wait2LinkEvent(Math.ceil(now), p.getDriver().getId(), p.getDriver().getCurrentLinkId(), Id.create(p.getDriver().getId(), Vehicle.class), "walkct", 0);
		this.em.processEvent(e);
		cell.jumpOnPed(p, now);
		//TODO move following to jumpoff method in cell; create pseudo cell class for that purpose
		LinkEnterEvent le = new LinkEnterEvent(Math.ceil(now), p.getDriver().getId(), p.getDriver().getCurrentLinkId(), Id.create(p.getDriver().getId(), Vehicle.class));
		this.em.processEvent(le);
		cell.updateIntendedCellJumpTimeAndChooseNextJumper(now);
	}

	private final class ProtoCell {
		public Map<GraphEdge, ProtoCell> nb = new HashMap<>();
		List<GraphEdge> edges = new ArrayList<>();
		private double x;
		private double y;
		private long id;

		public ProtoCell(double x, double y, long id) {
			this.x = x;
			this.y = y;
			this.id = id;
		}
	}

}
