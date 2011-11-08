package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;

public class PathAndDrivingAcceleration {

	private final PhysicalFloor floor;
	private HashMap<Id, LinkInfo> linkGeos;
	private HashMap<Id, Coordinate> drivingDirections;
	private final double tau;

	private static final double VIRTUAL_LENGTH = 1000;

	private static final double COS_RIGHT = Math.cos(-Math.PI / 2);
	private static final double SIN_RIGHT = Math.sin(-Math.PI / 2);

	// Mauron constant
	private static final double Apath =10;
	private static final double Bpath = .5;



	public PathAndDrivingAcceleration(PhysicalFloor floor) {
		this.floor = floor;
		this.tau = 0.5; //1/1.52;
		init();
	}

	public void init() {
		this.linkGeos = new HashMap<Id, LinkInfo>();
		GeometryFactory geofac = new GeometryFactory();
		for (Link link : this.floor.getLinks()) {
			Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
			Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());
			Coordinate c = new Coordinate(to.x - from.x, to.y - from.y);
			double length = Math.sqrt(Math.pow(c.x, 2) + Math.pow(c.y, 2));
			c.x /= length;
			c.y /= length;
			Coordinate virtualFrom = new Coordinate(from.x - c.x*VIRTUAL_LENGTH, from.y - c.y * VIRTUAL_LENGTH);
			LineString ls = geofac.createLineString(new Coordinate[] { virtualFrom, to });
			Coordinate perpendicularVec = new Coordinate(COS_RIGHT * c.x + SIN_RIGHT * c.y, -SIN_RIGHT * c.x + COS_RIGHT * c.y);
			LinkInfo li = new LinkInfo();
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

	public double [] getDesiredAccelerationForce(Agent2D agent) {


		Coordinate d = this.drivingDirections.get(agent.getCurrentLinkId());
		double driveX = d.x  * agent.getDesiredVelocity();
		double driveY = d.y * agent.getDesiredVelocity();


		double fdx = Agent2D.AGENT_WEIGHT *(driveX - agent.getForce().getVx())/this.tau;
		double fdy = Agent2D.AGENT_WEIGHT *(driveY - agent.getForce().getVy())/this.tau;

		Coordinate pos = agent.getPosition();
		LinkInfo li = this.linkGeos.get(agent.getCurrentLinkId());
		LineString ls = li.ls;

		double pathDist = MGC.xy2Point(agent.getPosition().x, agent.getPosition().y).distance(ls);
		double fpx = 0;
		double fpy = 0;
		if (pathDist > 0){
			double f = Apath * Math.exp(pathDist / Bpath);
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
		LineString ls;
		Coordinate perpendicularVector;
	}

}
