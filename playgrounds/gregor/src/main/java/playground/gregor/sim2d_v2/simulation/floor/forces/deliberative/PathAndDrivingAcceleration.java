package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.Collection;
import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import playground.gregor.sim2d_v2.scenario.MyDataContainer;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;

public class PathAndDrivingAcceleration {

	private final PhysicalFloor floor;
	private HashMap<Id, LinkInfo> linkGeos;
	private HashMap<Id, Coordinate> drivingDirections;
	private final double tau;
	private final Scenario sc;

	GeometryFactory geofac = new GeometryFactory();

	private static final double VIRTUAL_LENGTH = 1000;

	private static final double COS_RIGHT = Math.cos(-Math.PI / 2);
	private static final double SIN_RIGHT = Math.sin(-Math.PI / 2);

	// Mauron constant
	private static final double Apath =50;
	private static final double Bpath = 500;



	public PathAndDrivingAcceleration(PhysicalFloor floor, Scenario sc) {
		this.floor = floor;
		this.tau = 0.5; //1/1.52;
		this.sc = sc;
		init();
	}

	public void init() {
		this.linkGeos = new HashMap<Id, LinkInfo>();

		for (Link link : this.floor.getLinks()) {
			Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
			Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());
			Coordinate c = new Coordinate(to.x - from.x, to.y - from.y);
			double length = Math.sqrt(Math.pow(c.x, 2) + Math.pow(c.y, 2));
			c.x /= length;
			c.y /= length;
			Coordinate virtualFrom = new Coordinate(from.x - c.x*VIRTUAL_LENGTH, from.y - c.y * VIRTUAL_LENGTH);
			LineString ls = this.geofac.createLineString(new Coordinate[] { virtualFrom, to });
			Coordinate perpendicularVec = new Coordinate(COS_RIGHT * c.x + SIN_RIGHT * c.y, -SIN_RIGHT * c.x + COS_RIGHT * c.y);

			double minWidth = getMinWidth(this.geofac.createLineString(new Coordinate[]{from,to}),link.getCoord());
			LinkInfo li = new LinkInfo();
			li.pathWidth = minWidth;
			li.ls = ls;
			li.perpendicularVector = perpendicularVec;
			this.linkGeos.put(link.getId(), li);
		}
		this.drivingDirections = new HashMap<Id, Coordinate>();
		for (Link link : this.floor.getLinks()) {
			Coordinate c = new Coordinate(link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX(), link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY());
			double length = Math.sqrt(Math.pow(c.x, 2) + Math.pow(c.y, 2));
			c.x /= length;
			c.y /= length;
			this.drivingDirections.put(link.getId(), c);
		}
	}

	private double getMinWidth(LineString link, Coord coord) {
		QuadTree<Coordinate> q = this.sc.getScenarioElement(MyDataContainer.class).getQuadTree();
		Collection<Coordinate> coll = q.get(coord.getX(), coord.getY(), link.getLength());
		double minDist = Double.POSITIVE_INFINITY;
		for (Coordinate c : coll) {
			Point p = this.geofac.createPoint(c);
			double dist = p.distance(link);
			if (dist < minDist) {
				minDist = dist;
			}
		}
		return minDist;
	}

	public double [] getDesiredAccelerationForce(Agent2D agent) {


		Coordinate d = this.drivingDirections.get(agent.getCurrentLinkId());
		double driveX = d.x  * agent.getDesiredVelocity();
		double driveY = d.y * agent.getDesiredVelocity();

		//		Coordinate d2 = this.drivingDirections.get(agent.chooseNextLinkId());
		//		if (d2 != null) {
		//			double x = (d.x + d2.x);
		//			double y = (d.y + d2.y);
		//			double denom = Math.sqrt(x*x+y*y);
		//			driveX =  x/ denom* agent.getDesiredVelocity();
		//			driveY = y /denom * agent.getDesiredVelocity();
		//		}

		double fdx = Agent2D.AGENT_WEIGHT *(driveX - agent.getForce().getVx())/this.tau;
		double fdy = Agent2D.AGENT_WEIGHT *(driveY - agent.getForce().getVy())/this.tau;

		Coordinate pos = agent.getPosition();
		LinkInfo li = this.linkGeos.get(agent.getCurrentLinkId());
		double bpath = Math.max(0.1, li.pathWidth/2);

		LineString ls = li.ls;

		double pathDist = MGC.xy2Point(agent.getPosition().x, agent.getPosition().y).distance(ls);
		double fpx = 0;
		double fpy = 0;
		if (pathDist > bpath){
			double f = Apath * Math.exp(pathDist / bpath);
			Point orig = ls.getStartPoint();
			Point dest = ls.getEndPoint();
			double x2 = orig.getX() - dest.getX();
			double y2 = orig.getY() - dest.getY();
			double x3 = orig.getX() - pos.x;
			double y3 = orig.getY() - pos.y;
			boolean rightHandSide = x2*y3 - y2*x3 < 0 ? false : true;
			double dx = rightHandSide == true ? -li.perpendicularVector.x : li.perpendicularVector.x;
			double dy = rightHandSide == true ? -li.perpendicularVector.y : li.perpendicularVector.y;
			fpx  = dx * f;
			fpy = dy * f;
		}
		double fx = fdx + fpx;
		double fy = fdy + fpy;

		return new double []{fx,fy};
	}

	private static final class LinkInfo {
		double pathWidth;
		LineString ls;
		Coordinate perpendicularVector;
	}

}
