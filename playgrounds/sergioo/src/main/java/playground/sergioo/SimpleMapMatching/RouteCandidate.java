package playground.sergioo.SimpleMapMatching;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import util.geometry.Line2D;
import util.geometry.Point2D;

public class RouteCandidate {
	
	//Attributes
	/**
	 * The links of the route
	 */
	private List<LinkAttached> links;
	/**
	 * Score
	 */
	private double score;
	/**
	 * If the score is out of date
	 */
	private boolean modified;
	
	//Methods
	/**
	 * @param links
	 */
	public RouteCandidate() {
		super();
		this.links = new LinkedList<LinkAttached>();
		modified = false;
	}
	/**
	 * @return the links
	 */
	public List<LinkAttached> getLinks() {
		return links;
	}
	/**
	 * @return the last link
	 */
	public LinkAttached getLastLink() {
		if(links.size()>0)
			return links.get(links.size()-1);
		else
			return null;
	}
	/**
	 * @return the last link
	 */
	public LinkAttached getBeforeLastLink() {
		if(links.size()>1)
			return links.get(links.size()-2);
		else
			return null;
	}
	/**
	 * Adds a new link to the route
	 * @param link
	 * @param coord
	 * @return 
	 */
	public boolean addLink(Link link, int posCoord, List<Coord> points) {
		for(LinkAttached linkA:links)
			if(linkA.getLink().getId().equals(link.getId()))
				return false;
		links.add(new LinkAttached(link, posCoord));
		modified = true;
		return isGoodPoint(points.get(posCoord));
	}
	private void addLink(LinkAttached linkAttached) {
		links.add(linkAttached);
		modified = true;
	}
	/**
	 * Adds other point to the last link of the route
	 * @param point
	 */
	public void addPointToLastLink(Coord coord) {
		getLastLink().advance();
		modified = true;
	}
	public boolean isGoodPoint(Coord coord) {
		Link link = getLastLink().getLink();
		Point2D fromPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Point2D toPoint = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		Line2D linkLine = new Line2D(fromPoint, toPoint);
		Point2D point = new Point2D(coord.getX(),coord.getY()), nearest = linkLine.getNearestPoint(point);
		return linkLine.isInside2(nearest);
	}
	/**
	 * 
	 * @param points
	 */
	private void calculateScore(List<Coord> points) {
		score = 0;
		for(LinkAttached linkT:links) {
			score+=linkT.getScore(points);
		}
	}
	/**
	 * @return a previously calculated score of the routeCandidate
	 */
	public double getScore(List<Coord> points) {
		if(modified) {
			calculateScore(points);
			modified = false;
		}
		return score;
	}
	/**
	 * @param routeCandidate
	 * @return if the given route is equal to the actual one
	 */
	public boolean equals(RouteCandidate routeCandidate) {
		boolean equals = links.size()==routeCandidate.getLinks().size();
		Iterator<LinkAttached> r1=links.iterator();
		Iterator<LinkAttached> r2=routeCandidate.getLinks().iterator();
		while(equals && r1.hasNext() && r2.hasNext())
			if(!r1.next().equals(r2.next()))
				equals = false;
		return equals;
	}
	/**
	 * @return the distance of the set of points of the last link
	 */
	public double getPointsDistanceLastLink(List<Coord> points) {
		return getLastLink().getDistance(points);
	}
	/**
	 * @return a copy of the route candidate
	 */
	public RouteCandidate clone() {
		RouteCandidate clone = new RouteCandidate();
		for(LinkAttached link:links) {
			LinkAttached linkAttached = new LinkAttached(link.getLink(), link.getBeginIndex());
			linkAttached.setEndIndex(link.getEndIndex());
			clone.addLink(linkAttached);
		}
		return clone;
	}
	public int getNumPoints() {
		int num=0;
		for(LinkAttached link:links) {
			num+=link.getNunPoints();
		}
		return num;
	}
}
