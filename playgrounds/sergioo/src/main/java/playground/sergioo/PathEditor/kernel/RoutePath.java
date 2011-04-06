package playground.sergioo.PathEditor.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.AStarEuclidean;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessEuclidean;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

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
	private boolean withInsideStops = false;
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
	public RoutePath(Network network, Trip trip, Map<String, Stop> stops, List<Link> links) {
		super();
		this.network = network;
		this.trip = trip;
		this.stops = stops;
		this.links = links;
	}
	public double getMinDistance() {
		return minDistance;
	}
	public int getNumCandidates() {
		return numCandidates;
	}
	public List<Link> getLinks() {
		return links;
	}
	public Collection<Link> getStopLinks() {
		Collection<Link> links = new ArrayList<Link>();
		for(StopTime stopTime:trip.getStopTimes().values()) {
			String linkId = stops.get(stopTime.getStopId()).getLinkId();
			if(linkId!=null)
				links.add(network.getLinks().get(new IdImpl(linkId)));
		}
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
	public Coord getStop(String selectedStopId) {
		return stops.get(selectedStopId).getPoint();
	}
	public String getIdNearestStop(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		String nearest = "";
		double nearestDistance = Double.POSITIVE_INFINITY;
		for(StopTime stopTime:trip.getStopTimes().values()) {
			double distance = CoordUtils.calcDistance(stops.get(stopTime.getStopId()).getPoint(),coord);
			if(distance<nearestDistance) {
				nearest = stopTime.getStopId();
				nearestDistance = distance;
			}
		}
		return nearest;
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
	public void removeLinksFrom(int index) {
		int size=links.size();
		for(int i=index; i<size; i++)
			links.remove(index);
	}
	public void removeLinksTo(int index) {
		for(int i=0; i<=index; i++)
			links.remove(0);
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
		if(indexI<links.size()-1) {
			TravelTime timeFunction = new TravelTime() {	
				public double getLinkTravelTime(Link link, double time) {
					return link.getLength()/link.getFreespeed();
				}
			};
			LeastCostPathCalculator leastCostPathCalculator = new AStarEuclidean(network, preProcessData, timeFunction);
			Path path = leastCostPathCalculator.calcLeastCostPath(links.get(indexI).getToNode(), links.get(indexI+1).getFromNode(), 0);
			int i=1;
			for(Link link:path.links) {
				links.add(indexI+i,link);
				i++;
			}
		}
	}
	public void calculatePath() {
		links.clear();
		TravelTime timeFunction = new TravelTime() {	
			public double getLinkTravelTime(Link link, double time) {
				return link.getLength()/link.getFreespeed();
			}
		};
		Iterator<StopTime> prev=trip.getStopTimes().values().iterator(), next=trip.getStopTimes().values().iterator();
		List<Link> prevLL, nextLL;
		Link prevL = null;
		next.next();
		Stop prevStop = null,nextStop = null;
		if(next.hasNext()) {
			Path bestPath=null;
			for(int numCandidates = this.numCandidates;bestPath==null;numCandidates++) {
				prevStop = stops.get(prev.next().getStopId());
				nextStop = stops.get(next.next().getStopId());
				if(prevStop.getLinkId()!=null) {
					prevLL=new ArrayList<Link>();
					prevLL.add(network.getLinks().get(new IdImpl(prevStop.getLinkId())));
				}
				else
					prevLL=getBestLinksMode(network,"Car", prevStop.getPoint(),trip.getShape());
				if(nextStop.getLinkId()!=null) {
					nextLL=new ArrayList<Link>();
					nextLL.add(network.getLinks().get(new IdImpl(nextStop.getLinkId())));
				}
				else
					nextLL=getBestLinksMode(network,"Car", nextStop.getPoint(),trip.getShape());
				List<Tuple<Path,Link[]>> paths = new ArrayList<Tuple<Path,Link[]>>();
				for(int i=0; i<prevLL.size(); i++)
					for(int j=0; j<nextLL.size(); j++) {
						LeastCostPathCalculator leastCostPathCalculator = new AStarEuclidean(network, preProcessData, timeFunction);
						Path path = leastCostPathCalculator.calcLeastCostPath(prevLL.get(i).getFromNode(), nextLL.get(j).getToNode(), 0);
						if(path.links.contains(prevLL.get(i)) && path.links.contains(nextLL.get(j)))
							paths.add(new Tuple<Path,Link[]>(path,new Link[]{prevLL.get(i),nextLL.get(j)}));
					}
				double shortestDistance = Double.POSITIVE_INFINITY;
				for(Tuple<Path,Link[]> tuple:paths) {
					if(tuple.getFirst().links.size()>0) {
						double distance = calculateDistance(tuple.getFirst(),prevStop.getPoint(),nextStop.getPoint(),prevL);
						if(bestPath==null||distance<shortestDistance) {
					 		shortestDistance = distance;
					 		bestPath = tuple.getFirst();
					 		prevStop.setLinkId(tuple.getSecond()[0].getId().toString());
					 		nextStop.setLinkId(tuple.getSecond()[1].getId().toString());
					 	}
					}
				}
			}	
			links.addAll(bestPath.links);
			prevL = bestPath.links.get(bestPath.links.size()-1);
			prevStop = nextStop;
		}
		for(;next.hasNext();) {
			Path bestPath=null;
			for(int numCandidates = this.numCandidates;bestPath==null;numCandidates++) {
				nextStop = stops.get(next.next().getStopId());
				if(nextStop.getLinkId()!=null) {
					nextLL=new ArrayList<Link>();
					nextLL.add(network.getLinks().get(new IdImpl(nextStop.getLinkId())));
				}
				else
					nextLL=getBestLinksMode(network,"Car", nextStop.getPoint(),trip.getShape());
				List<Tuple<Path,Link>> paths = new ArrayList<Tuple<Path,Link>>();
				for(int i=0; i<nextLL.size(); i++) {
					LeastCostPathCalculator leastCostPathCalculator = new AStarEuclidean(network, preProcessData, timeFunction);
					Path path = leastCostPathCalculator.calcLeastCostPath(prevL.getToNode(), nextLL.get(i).getToNode(), 0);
					if(path.links.contains(nextLL.get(i)))
						paths.add(new Tuple<Path,Link>(path,nextLL.get(i))); 
				}	
				double shortestDistance = Double.POSITIVE_INFINITY;
				for(Tuple<Path,Link> tuple:paths) {
					double distance = calculateDistance(tuple.getFirst(),prevStop.getPoint(),nextStop.getPoint(),prevL);
					if(bestPath==null||distance<shortestDistance) {
						shortestDistance = distance;
					 	bestPath = tuple.getFirst();
					 	nextStop.setLinkId(tuple.getSecond().getId().toString());
					}
				}
			}
			links.addAll(bestPath.links);
			prevL = bestPath.links.get(bestPath.links.size()-1);
			prevStop = nextStop;
		}
	}
	private List<Link> getBestLinksMode(Network network, String mode, Coord coord, Shape shape) {
		List<Double> nearestDistances = new ArrayList<Double>();
		List<Link> nearestLinks = new ArrayList<Link>();
		for(Link link:network.getLinks().values())
			if(link.getAllowedModes().contains(mode)) {
				Point2D fromPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
				Point2D toPoint = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
				Vector2D linkVector = new Vector2D(fromPoint, toPoint);
				Line2D linkSegment = new Line2D(fromPoint, toPoint);
				if(!withInsideStops || linkSegment.isNearestInside(new Point2D(coord.getX(),coord.getY())))
					if(!withAngleShape || shape==null || linkVector.getAngleTo(shape.getVector(coord))<Math.PI/16) {
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
	private double calculateDistance(Path path, Coord prevStop, Coord nextStop, Link prevL) {
		if(prevL!=null)
			path.links.add(0,prevL);
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
		path.links.remove(0);
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
	public void initStops() {
		for(StopTime stopTime: trip.getStopTimes().values())
			stops.get(stopTime.getStopId()).setLinkId(null);
	}
	public void addLinkStop(int selectedLinkIndex, String selectedStopId) {
		stops.get(selectedStopId).setLinkId(getLink(selectedLinkIndex).getId().toString());
	}
	public void removeLinkStop(String selectedStopId) {
		stops.get(selectedStopId).setLinkId(null);
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
