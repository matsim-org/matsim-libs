package playground.sergioo.SimpleMapMatching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

import util.geometry.Line2D;
import util.geometry.Point2D;
import util.geometry.Vector2D;
import visUtils.PointLines;
import visUtils.Window;

public class SimpleMapMatcher extends Observable implements PointLines {
	
	//Constants
	private static double INITIAL_SEARCH_RADIUS = 750/6371000;
	private static int MIN_NUMBER_START_NODES = 5;
	private static double SEARCH_RADIUS_INCREMENT = 100.0/6371000;
	private static int MAX_NUMBER_OF_CANDIDATES = 35;
	private static double SEUDO_ORTHOGONAL_ANGLE = 85*Math.PI/180;
	//Attributes
	/**
	 * The original two-dimensional points
	 */
	private List<Coord> points;
	/**
	 * The network in which the pints must me mapped
	 */
	private Network network;
	/**
	 * The candidates
	 */
	private List<List<RouteCandidate>> routeCandidates;
	/**
	 * The mode of the desired path
	 */
	private String mode;
	//Methods
	/**
	 * 
	 * @param
	 */
	public SimpleMapMatcher(List<Coord> points, Network network, String mode) {
		this.points = points;
		this.network = network;
		this.mode = mode;
		routeCandidates = new ArrayList<List<RouteCandidate>>();
		Window window = new Window(this);
		window.setVisible(true);
		run();
	}
	/**
	 * Calculates the routes that fit with the given points
	 */
	private void run() {
		List<RouteCandidate> newRouteCandidates = new ArrayList<RouteCandidate>();
		for(int pointN =0;pointN<points.size();pointN++) {
			System.out.println(pointN);
			Coord point =  points.get(pointN);
			if(newRouteCandidates.size()<2) {
				Collection<Node> initialNodes=new ArrayList<Node>();
				for(double addDistance=0;initialNodes.size()<MIN_NUMBER_START_NODES;addDistance+=SEARCH_RADIUS_INCREMENT)
					initialNodes=((NetworkImpl)network).getNearestNodes(point, INITIAL_SEARCH_RADIUS+addDistance);
				for(Link link:((NetworkImpl)network).getLinks().values())
					if(link.getAllowedModes().contains(mode)) {
						boolean haveInitialNode = false;
						for(Iterator<Node> iNode = initialNodes.iterator();iNode.hasNext()&&!haveInitialNode;) {
							Node node = iNode.next();
							if(link.getFromNode().equals(node)||link.getToNode().equals(node)) {
								RouteCandidate routeCandidate = new RouteCandidate();
								if(routeCandidate.addLink(link,pointN,points))
									addRouteCandidate(newRouteCandidates,routeCandidate);
								haveInitialNode = true;
							}
						}
					}
				routeCandidates.add(newRouteCandidates);
			}
			else {
				newRouteCandidates = new ArrayList<RouteCandidate>();
				for(RouteCandidate routeCandidate:routeCandidates.get(routeCandidates.size()-1))
					if(!(routeCandidate.getLinks().size()==1 && this.reachedStartOfLastLink(routeCandidate, point)))
						if(this.reachedEndOfLastLink(routeCandidate,pointN)) {
							for(Link link:((NetworkImpl)network).getLinks().values())
								if(link.getAllowedModes().contains(mode) && link.getFromNode().equals(routeCandidate.getLastLink().getLink().getToNode())) {
									RouteCandidate newRouteCandidate = routeCandidate.clone();
									if(newRouteCandidate.addLink(link, pointN, points))
										addRouteCandidate(newRouteCandidates,newRouteCandidate);
								}
						}
						else {
							routeCandidate.addPointToLastLink(point);
							addRouteCandidate(newRouteCandidates,routeCandidate);
						}
				if(newRouteCandidates.size()>1)
					routeCandidates.set(routeCandidates.size()-1, newRouteCandidates);
				else
					pointN--;
			}
		}
	}
	/**
	 * @return the sequence of links with the best score that maps the original points
	 */
	public List<Link> getBestRoute() {
		RouteCandidate best = routeCandidates.get(routeCandidates.size()-1).get(0);
		List<Link> links = new LinkedList<Link>();
		int bad=0;
		for(LinkAttached link:best.getLinks()) {
			if(link.isBad())
				++bad;
			links.add(link.getLink());
		}
		System.out.println(best.getLinks().size()+" "+bad);
		return links;
	}
	/**
	 * Adds a new route candidate if is not repeated and if is valid. If the max number of candidates is reached the worst route is deleted 
	 * @param routeCandidate
	 */
	private void addRouteCandidate(List<RouteCandidate> routeCandidates, RouteCandidate routeCandidate) {
		double actualScore = routeCandidate.getScore(points);
		if(routeCandidates.isEmpty() || actualScore<routeCandidates.get(0).getScore(points)) {
			this.setChanged();
			this.notifyObservers();
		}
		if((routeCandidates.isEmpty() || !isRepeated(routeCandidates,routeCandidate)) && isValid(routeCandidate)) {
			if(routeCandidates.size()==MAX_NUMBER_OF_CANDIDATES)
				routeCandidates.remove(routeCandidates.size()-1);
			boolean added = false;
			for(int i=0; !added && i<routeCandidates.size(); i++)
				if(routeCandidates.get(i).getScore(points)>actualScore) {
					routeCandidates.add(i,routeCandidate);
					added = true;
				}
			if(!added)
				routeCandidates.add(routeCandidate);
		}
	}
	/**
	 * @param routeCandidate
	 * @return if the given route candidate has the same link sequences of any route candidate
	 */
	private boolean isRepeated(List<RouteCandidate> routeCandidates, RouteCandidate routeCandidate) {
		for(RouteCandidate routeCandidate2:routeCandidates)
			if(routeCandidate.equals(routeCandidate2))
				return true;
		return false;
	}
	/**
	 * @param routeCandidate
	 * @return if the given route candidate is valid
	 */
	private boolean isValid(RouteCandidate routeCandidate) {
		LinkAttached lastLink = routeCandidate.getLastLink();
		LinkAttached beforeLastLink = routeCandidate.getBeforeLastLink();
		return !((lastLink!=null && beforeLastLink!=null && lastLink.getLink().getToNode().equals(beforeLastLink.getLink().getFromNode())));// ||routeCandidate.getNumPoints()!=pointN);
	}
	/**
	 * 
	 * @param link
	 * @param coord
	 * @return
	 */
	private boolean reachedStartOfLastLink(RouteCandidate routeCandidate, Coord coord) {
		Link link = routeCandidate.getLastLink().getLink();
		Point2D point = new Point2D(coord.getX(),coord.getY());
		Point2D fromPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Point2D toPoint = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		Vector2D linkSegment = new Vector2D(fromPoint, toPoint);
		Vector2D pointSegment = new Vector2D(fromPoint, point);		
		return linkSegment.dotProduct(pointSegment)<0;
	}
	private boolean reachedEndOfLastLink(RouteCandidate routeCandidate, int posCoord) {
		Coord coord = points.get(posCoord);
		Link link = routeCandidate.getLastLink().getLink();
		Point2D point = new Point2D(coord.getX(),coord.getY());
		Point2D fromPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Point2D toPoint = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		Vector2D linkSegment = new Vector2D(fromPoint, toPoint);
		Vector2D pointSegment = new Vector2D(toPoint, point);
		Coord other = posCoord+1==points.size()? points.get(posCoord-1):points.get(posCoord+1);
		return linkSegment.dotProduct(pointSegment)>0 ||
				linkSegment.getAngleTo(new Vector2D(point, new Point2D(other.getX(), other.getY())))>SEUDO_ORTHOGONAL_ANGLE ||
				routeCandidate.getPointsDistanceLastLink(points)+pointSegment.getMagnitude()>link.getLength();
	}
	@Override
	public Collection<Point2D> getPoints() {
		Collection<Point2D> ps = new HashSet<Point2D>();
		for(int i=0; i<points.size(); i++)
			ps.add(new Point2D(points.get(i).getX(), points.get(i).getY()));
		return ps;
	}
	@Override
	public Collection<Line2D> getLines() {
		if(routeCandidates.isEmpty())
			return new HashSet<Line2D>();
		else {
			Collection<Line2D> ls = new HashSet<Line2D>();
			for(List<RouteCandidate> routeCandidatesA:routeCandidates) {
				List<LinkAttached> links = routeCandidatesA.get(0).getLinks();
				for(LinkAttached linkA:links) {
					Link link = linkA.getLink();
					ls.add(new Line2D(new Point2D(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY()),new Point2D(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY())));
				}
			}
			return ls;
		}
	}
	public Collection<Line2D> getLines2() {
		Collection<Line2D> ls = new HashSet<Line2D>();
		for(Link link:network.getLinks().values())
			ls.add(new Line2D(new Point2D(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY()),new Point2D(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY())));
		return ls;
	}
}
