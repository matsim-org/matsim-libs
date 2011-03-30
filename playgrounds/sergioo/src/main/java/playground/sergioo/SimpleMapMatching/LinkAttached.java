package playground.sergioo.SimpleMapMatching;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;

import util.geometry.Line2D;
import util.geometry.Point2D;

public class LinkAttached {
	
	//Constants
	private static final double DISTANCE_W = 1;
	private static final double BORDERS_W = 1;
	
	//Attributes
	private Link link;
	private int beginIndex;
	private int endIndex;

	//Methods
	/**
	 * @param link
	 * @param beginIndex
	 */
	public LinkAttached(Link link, int beginIndex) {
		super();
		this.link = link;
		this.beginIndex = beginIndex;
		this.endIndex = beginIndex;
	}
	/**
	 * @return the link
	 */
	public Link getLink() {
		return link;
	}
	/**
	 * @return the beginIndex
	 */
	public int getBeginIndex() {
		return beginIndex;
	}
	/**
	 * @return the endIndex
	 */
	public int getEndIndex() {
		return endIndex;
	}
	/**
	 * @param endIndex the endIndex to set
	 */
	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}
	/**
	 * Advance the endIndex
	 */
	public void advance() {
		endIndex++;
	}
	/**
	 * @return the score of the link
	 */
	public double getScore(List<Coord> points) {
		Point2D fromPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Point2D toPoint = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		Line2D linkLine = new Line2D(fromPoint, toPoint);
		double distance = 0;
		for(int p=beginIndex; p<=endIndex; p++) {
			Point2D point = new Point2D(points.get(p).getX(),points.get(p).getY());
			distance+=linkLine.getDistanceToPoint(point);
		}
		double borders = beginIndex==0?0:fromPoint.getDistance(new Point2D(points.get(beginIndex-1).getX(),points.get(beginIndex-1).getY()));
		borders += endIndex==points.size()-1?0:toPoint.getDistance(new Point2D(points.get(endIndex+1).getX(),points.get(endIndex+1).getY()));
		return DISTANCE_W*distance + BORDERS_W*borders;
	}
	/**
	 * @return if the link is the same than the given one
	 */
	public boolean equals(LinkAttached other) {
		return link.getId().equals(other.getLink().getId());
	}
	public double getDistance(List<Coord> points) {
		double distance = 0;
		for(int p=beginIndex; p<endIndex; p++)
			distance+=CoordUtils.calcDistance(points.get(p),points.get(p+1));
		return distance;
	}
	public int getNunPoints() {
		return endIndex-beginIndex+1;
	}
	public boolean isBad() {
		return false;
	}
}
