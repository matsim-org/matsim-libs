package playground.sergioo.PathEditor.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.AStarEuclidean;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessEuclidean;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.sergioo.GTFS.Shape;
import playground.sergioo.GTFS.Stop;
import playground.sergioo.GTFS.StopTime;
import playground.sergioo.GTFS.Trip;
import util.geometry.Line2D;
import util.geometry.Point2D;
import util.geometry.Vector2D;

public class RoutePath {
	
	//Constants
	private static final double MIN_DISTANCE_DELTA = 50*180/(6371000*Math.PI);
	
	//Attributes
	private Map<String, Stop> stops;
	public List<Link> links;
	private final Network network;
	private final Trip trip;
	//Parameters
	private double minDistance = 300*180/(6371000*Math.PI);
	private int numCandidates = 6;
	private boolean withAngleShape = false;
	private boolean withCostShape = false;
	private PreProcessEuclidean preProcessData;
	
	//Methods
	public RoutePath(Network network, Trip trip, Map<String, Stop> stops) {
		super();
		this.network = network;
		this.trip = trip;
		this.stops = stops;
		links = new ArrayList<Link>();
		TravelMinCost travelMinCost = new TravelMinCost() {
			public double getLinkGeneralizedTravelCost(Link link, double time) {
				return getLinkMinimumTravelCost(link);
			}
			public double getLinkMinimumTravelCost(Link link) {
				return link.getLength();
			}
		};
		preProcessData = new PreProcessEuclidean(travelMinCost);
		preProcessData.run(network);
		calculatePath();
	}
	public List<Link> getLinks() {
		return links;
	}
	public Collection<Coord> getShapePoints() {
		return trip.getShape().getPoints().values();
	}
	public Collection<Coord> getStopPoints() {
		Collection<Coord> points = new ArrayList<Coord>();
		for(StopTime stopTime:trip.getStopTimes().values())
			points.add(stops.get(stopTime.getStopId()).getPoint());
		return points;
	}
	public Link getLink(int index) {
		return links.get(index);
	}
	public int getIndexNearestLink(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		int nearest = -1;
		double nearestDistance = Double.POSITIVE_INFINITY;
		for(int i=0; i<links.size(); i++) {
			double distance = ((LinkImpl) links.get(i)).calcDistance(coord); 
			if(distance<nearestDistance) {
				nearest = i;
				nearestDistance = distance;
			}
		}
		return nearest;
	}
	public void removeLink(int index) {
		links.remove(index);
	}
	public void addLink(int index, Coord second) {
		Link link = links.get(index);
		if(index==links.size()-1||!link.getToNode().equals(links.get(index+1).getFromNode())) {
			Point2D toPoint = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			Point2D secondPoint = new Point2D(second.getX(), second.getY());
			Vector2D dirSegment = new Vector2D(toPoint, secondPoint);
			Link bestLink = null;
			double smallestAngle = Double.POSITIVE_INFINITY;
			for(Link linkN:network.getLinks().values())
				if(link.getToNode().equals(linkN.getFromNode())) {
					Point2D toPoint2 = new Point2D(linkN.getToNode().getCoord().getX(), linkN.getToNode().getCoord().getY());
					Vector2D linkNSegment = new Vector2D(toPoint, toPoint2);
					double angle = dirSegment.getAngleTo(linkNSegment);
					if(angle<smallestAngle) {
						smallestAngle = angle;
						bestLink = linkN;
					}
				}
			links.add(index+1, (LinkImpl)bestLink);
		}	
	}
	public void addShortestPath(int indexI) {
		//TODO
	}
	public void calculatePath() {
		TravelTime timeFunction = new TravelTime() {	
			public double getLinkTravelTime(Link link, double time) {
				return link.getLength()/link.getFreespeed();
			}
		};
		Iterator<StopTime> prev=trip.getStopTimes().values().iterator(), next=trip.getStopTimes().values().iterator();
		List<Link> prevLL, nextLL;
		Link prevL = null;
		next.next();
		CoordImpl prevStop = null,nextStop = null;
		if(next.hasNext()) {
			prevStop = (CoordImpl) stops.get(prev.next().getStopId()).getPoint();
			nextStop = (CoordImpl) stops.get(next.next().getStopId()).getPoint();	
			prevLL=getBestLinksMode(network,"Car", prevStop,trip.getShape());
			nextLL=getBestLinksMode(network,"Car", nextStop,trip.getShape());
			List<Path> paths = new ArrayList<Path>();
			for(int i=0; i<prevLL.size(); i++)
				for(int j=0; j<nextLL.size(); j++) {
					LeastCostPathCalculator leastCostPathCalculator = new AStarEuclidean(network, preProcessData, timeFunction);
					paths.add(leastCostPathCalculator.calcLeastCostPath(prevLL.get(i).getToNode(), nextLL.get(j).getToNode(), 0));
				}
			Path bestPath=null;
			double shortestDistance = Double.POSITIVE_INFINITY;
			for(Path path:paths) {
				if(path.links.size()>0) {
					double distance = calculateDistance(path,prevStop,nextStop,prevL);
					if(bestPath==null||distance<shortestDistance) {
				 		shortestDistance = distance;
				 		bestPath = path;
				 	}
				}
				else
					System.out.println();
			}
			links.addAll(bestPath.links);
			prevL = bestPath.links.get(bestPath.links.size()-1);
			prevStop = nextStop;
		}
		for(;next.hasNext();) {
			nextStop = (CoordImpl) stops.get(next.next().getStopId()).getPoint();	
			nextLL=getBestLinksMode(network,"Car", nextStop,trip.getShape());
			List<Path> paths = new ArrayList<Path>();
			for(int i=0; i<nextLL.size(); i++) {
				LeastCostPathCalculator leastCostPathCalculator = new AStarEuclidean(network, preProcessData, timeFunction);
				paths.add(leastCostPathCalculator.calcLeastCostPath(prevL.getToNode(), nextLL.get(i).getToNode(), 0));
			}	
			Path bestPath=null;
			double shortestDistance = Double.POSITIVE_INFINITY;
			for(Path path:paths) {
				double distance = calculateDistance(path,prevStop,nextStop,prevL);
				if(bestPath==null||distance<shortestDistance) {
					shortestDistance = distance;
				 	bestPath = path;
				}
			}
			links.addAll(bestPath.links);
			prevL = bestPath.links.get(bestPath.links.size()-1);
			prevStop = nextStop;
		}
	}
	private List<Link> getBestLinksMode(Network network, String mode, CoordImpl coord, Shape shape) {
		List<Double> nearestDistances = new ArrayList<Double>();
		List<Link> nearestLinks = new ArrayList<Link>();
		for(Link link:network.getLinks().values())
			if(link.getAllowedModes().contains(mode)) {
				Point2D fromPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
				Point2D toPoint = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
				Vector2D linkSegment = new Vector2D(fromPoint, toPoint);
				if(!withAngleShape || shape==null || linkSegment.getAngleTo(shape.getVector(coord))<Math.PI/16) {
					double distance = ((LinkImpl)link).calcDistance(coord);
					if(distance<minDistance) {
						int i=0;
						for(; i<nearestDistances.size() && distance<nearestDistances.get(i); i++);
						if(i>0 || nearestLinks.size()<numCandidates) {
							nearestDistances.add(i, distance);
							nearestLinks.add(i, link);
							if(nearestLinks.size()>numCandidates) {
								nearestDistances.remove(0);
								nearestLinks.remove(0);
							}
						}
					}
				}
			}
		return nearestLinks;	
	}
	private double calculateDistance(Path path, CoordImpl prevStop, CoordImpl nextStop, Link prevL) {
		if(path.links.size()==0)
			path.links.add(prevL);
		LinkImpl firstLink = (LinkImpl)path.links.get(0);
		Point2D fromPoint = new Point2D(firstLink.getFromNode().getCoord().getX(), firstLink.getFromNode().getCoord().getY());
		Point2D toPoint = new Point2D(firstLink.getToNode().getCoord().getX(), firstLink.getToNode().getCoord().getY());
		Line2D firstLinkLine = new Line2D(fromPoint, toPoint);
		LinkImpl lastLink = (LinkImpl)path.links.get(path.links.size()-1);
		Point2D fromPoint2 = new Point2D(lastLink.getFromNode().getCoord().getX(), lastLink.getFromNode().getCoord().getY());
		Point2D toPoint2 = new Point2D(lastLink.getToNode().getCoord().getX(), lastLink.getToNode().getCoord().getY());
		Line2D lastLinkLine = new Line2D(fromPoint2, toPoint2);
		double distance = firstLink.calcDistance(prevStop);
		Coord firstToNodeCoord = firstLink.getToNode().getCoord();
		distance += firstLinkLine.getNearestPoint(new Point2D(prevStop.getX(), prevStop.getY())).getDistance(new Point2D(firstToNodeCoord.getX(),firstToNodeCoord.getY()));
		for(int i=1; i<path.links.size()-1; i++)
			distance += path.links.get(i).getLength();
		Coord lastFromNodeCoord = lastLink.getFromNode().getCoord();
		distance += lastLinkLine.getNearestPoint(new Point2D(nextStop.getX(), nextStop.getY())).getDistance(new Point2D(lastFromNodeCoord.getX(),lastFromNodeCoord.getY()));
		distance += lastLink.calcDistance(nextStop);
		return distance;
	}
	public int isPathJoined() {
		for(int i=0; i<links.size()-2; i++)
			if(!links.get(i).getToNode().equals(links.get(i+1).getFromNode()))
				return i;
		return -1;
	}
	public void increaseMinDistance() {
		minDistance += MIN_DISTANCE_DELTA;
	}
	public void decreaseMinDistance() {
		if(minDistance-MIN_DISTANCE_DELTA>0)
			minDistance -= MIN_DISTANCE_DELTA;
	}
	public void increaseNumCandidates() {
		numCandidates ++;
	}
	public void decreaseNumCandidates() {
		if(numCandidates-1>0)
			numCandidates --;
	}
	public void setWithAngleShape() {
		withAngleShape = !withAngleShape;
	}
	public void setWithCostShape() {
		TravelMinCost travelMinCost = null;
		if(withCostShape) {
			travelMinCost = new TravelMinCost() {
				public double getLinkGeneralizedTravelCost(Link link, double time) {
					return getLinkMinimumTravelCost(link);
				}
				public double getLinkMinimumTravelCost(Link link) {
					return link.getLength();
				}
			};
			preProcessData = new PreProcessEuclidean(travelMinCost);
			preProcessData.run(network);
		}
		else if(!withCostShape) {
			travelMinCost = new TravelMinCost() {
				public double getLinkGeneralizedTravelCost(Link link, double time) {
					return getLinkMinimumTravelCost(link);
				}
				public double getLinkMinimumTravelCost(Link link) {
					return link.getLength()*trip.getShape().getDistance(link);
				}
			};
			preProcessData = new PreProcessEuclidean(travelMinCost);
			preProcessData.run(network);
		}
		withCostShape = !withCostShape;
	}
	
}
