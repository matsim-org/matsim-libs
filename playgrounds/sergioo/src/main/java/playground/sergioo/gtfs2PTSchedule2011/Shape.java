package playground.sergioo.gtfs2PTSchedule2011;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;

import others.sergioo.util.geometry.Point2D;
import others.sergioo.util.geometry.Vector2D;


public class Shape {
	
	//Attributes
	/**
	 * The id
	 */
	private String id;
	/**
	 * The points of the shape
	 */
	private SortedMap<Integer,Coord> points;
	
	//Methods
	/**
	 * Constructs 
	 */
	public Shape(String id) {
		this.id = id;
		points = new TreeMap<Integer,Coord>();
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @return the points
	 */
	public SortedMap<Integer,Coord> getPoints() {
		return points;
	}
	/**
	 * Adds a new point
	 * @param point
	 */
	public void addPoint(Coord point, int pos) {
		points.put(pos,point);
	}
	/**
	 * @param point
	 * @return the direction vector of the shape related to the point
	 */
	public Vector2D getVector(Coord point) {
		double nearestDistance = Double.POSITIVE_INFINITY;
		int nearestPointPos = -1;
		for(int p=1; p<=points.size(); p++) {
			double distance = CoordUtils.calcEuclideanDistance(point, points.get(p));
			if(distance<nearestDistance) {
				nearestDistance = distance;
				nearestPointPos = p;
			}
		}
		Coord prev = nearestPointPos==1?null:points.get(nearestPointPos-1);
		Coord next = nearestPointPos==points.size()?null:points.get(nearestPointPos+1);
		Vector2D v1 = prev==null?new Vector2D():new Vector2D(new Point2D(prev.getX(), prev.getY()), new Point2D(point.getX(), point.getY()));
		Vector2D v2 = next==null?new Vector2D():new Vector2D(new Point2D(point.getX(), point.getY()), new Point2D(next.getX(), next.getY()));
		return v1.getSum(v2);
	}
	public double getDistance(Link link) {
		double nearestDistance = Double.POSITIVE_INFINITY;
		for(Coord coord:points.values()) {
			final Coord coord1 = coord;
			Link r = ((Link)link);
			double distance = CoordUtils.distancePointLinesegment(r.getFromNode().getCoord(), r.getToNode().getCoord(), coord1);
			if(distance<nearestDistance)
				nearestDistance = distance;
		}
		return nearestDistance;
	}
	public double getAngle(Link link) {
		Point2D fromPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Point2D toPoint = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		Vector2D linkVector = new Vector2D(fromPoint, toPoint);
		Vector2D shapeVector = getVector(link.getCoord());
		return linkVector.getAngleTo(shapeVector);
	}
}
