package playground.sergioo.PathEditor.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
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
	private static final double MIN_DISTANCE_DELTA = 20*180/(6371000*Math.PI);
	
	//Attributes
	private Map<String, Stop> stops;
	public List<Link> links;
	private final Network network;
	private final Trip trip;
	//Parameters
	private double minDistance = 40*180/(6371000*Math.PI);
	private int numCandidates = 3;
	private boolean withAngleShape = false;
	private boolean withShapeCost = false;
	private boolean withInsideStops = true;
	private boolean us = true;
	private boolean reps = true;
	private boolean inStops = true;
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
	public boolean isWithAngleShape() {
		return withAngleShape;
	}
	public void setWithAngleShape() {
		this.withAngleShape = !withAngleShape;
	}
	public boolean isWithShapeCost() {
		return withShapeCost;
	}
	public boolean isWithInsideStops() {
		return withInsideStops;
	}
	public void setWithInsideStops() {
		this.withInsideStops = !withInsideStops;
	}
	public boolean isUs() {
		return us;
	}
	public void setUs() {
		us = !us;
	}
	public boolean isReps() {
		return reps;
	}
	public void setReps() {
		reps = !reps;
	}
	public boolean isInStops() {
		return inStops;
	}
	public void setInStops() {
		inStops = !inStops;
	}
	public String getUsText() {
		return "Us "+us;
	}
	public String getRepsText() {
		return "Reps "+reps;
	}
	public String getInsideStopsText() {
		return "InStops "+inStops;
	}
	public String getLinkText() {
		return "";
	}
	public String getStopText() {
		return "";
	}
	public String getMinDistanceText() {
		return Math.round(minDistance*6371000*Math.PI/180)+"";
	}
	public double getMinDistance() {
		return minDistance;
	}
	public int getNumCandidates() {
		return numCandidates;
	}
	public String getNumCandidatesText() {
		return numCandidates+"";
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
	public Collection<Link> getNetworkLinks(double xMin, double yMin, double xMax, double yMax) {
		Collection<Link> links =  new HashSet<Link>();
		for(Link link:network.getLinks().values()) {
			Coord linkCenter = link.getCoord();
			if(xMin-10*minDistance<linkCenter.getX()&&yMin-10*minDistance<linkCenter.getY()&&xMax+10*minDistance>linkCenter.getX()&&yMax+10*minDistance>linkCenter.getY())
				links.add(link);
		}
		return links;
	}
	public Collection<Coord> getShapePoints() {
		if(trip.getShape()!=null)
			return trip.getShape().getPoints().values();
		else
			return new ArrayList<Coord>();
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
	public int getIndexStop(String selectedStopId) {
		int i=0;
		for(StopTime stopTime:trip.getStopTimes().values()) {
			if(stopTime.getStopId().equals(selectedStopId))
				return i;
			i++;
		}
		return -1;
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
	public Node getNearestNode(double x, double y) {
		Coord point = new CoordImpl(x, y);
		Node nearest = links.get(0).getFromNode();
		double nearestDistance = CoordUtils.calcDistance(point, nearest.getCoord());
		for(Link link:links) {
			double distance = CoordUtils.calcDistance(new CoordImpl(x, y),link.getToNode().getCoord());
			if(distance<nearestDistance) {
				nearestDistance = distance;
				nearest = link.getToNode();
			}
		}
		return nearest;
	}
	public void addLinkFirst(Coord point) {
		Link nearest = null;
		double nearestDistance = Double.POSITIVE_INFINITY;
		for(Link link:network.getLinks().values()) {
			double distance = ((LinkImpl)link).calcDistance(point);
			if(distance<nearestDistance) {
				nearestDistance = distance;
				nearest = link;
			}
		}
		links.add(0,nearest);
	}
	public void addLinkNext(int index, Coord second) {
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
	public void addLinkNetwork(Node selectedNode, Coord second) {
		network.addLink(network.getFactory().createLink(new IdImpl(network.getLinks().size()*2), selectedNode, getNearestNode(second.getX(), second.getY())));
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
		Link prevL = null, nextL = null;
		next.next();
		Stop prevStop = null,nextStop = null;
		if(next.hasNext()) {
			Path bestPath=null;
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
					prevL = prevLL.get(i);
					nextL = nextLL.get(j);
					Path path;
					if(prevL==nextL)
						path = new Path(new ArrayList<Node>(), new ArrayList<Link>(), 0.0, 0.0);
					else {
						LeastCostPathCalculator leastCostPathCalculator = new AStarEuclidean(network, preProcessData, timeFunction);
						path = leastCostPathCalculator.calcLeastCostPath(prevL.getToNode(), nextL.getFromNode(), 0);
						path.links.add(0,prevL);
					}
					path.links.add(nextL);
					paths.add(new Tuple<Path,Link[]>(path,new Link[]{prevL,nextLL.get(j)}));
				}
			double shortestDistance = Double.POSITIVE_INFINITY;
			for(Tuple<Path,Link[]> tuple:paths) {
				if(tuple.getFirst().links.size()>0) {
					double distance = calculateDistance(tuple.getFirst(),prevStop.getPoint(),nextStop.getPoint());
					if(bestPath==null||distance<=shortestDistance) {
				 		shortestDistance = distance;
				 		bestPath = tuple.getFirst();
				 		prevStop.setLinkId(tuple.getSecond()[0].getId().toString());
				 		nextStop.setLinkId(tuple.getSecond()[1].getId().toString());
				 	}
				}
			}
			prevL = bestPath.links.get(bestPath.links.size()-1);
			links.addAll(bestPath.links);
			prevStop = nextStop;
		}
		for(;next.hasNext();) {
			Path bestPath=null;
			nextStop = stops.get(next.next().getStopId());
			if(nextStop.getLinkId()!=null) {
				nextL = network.getLinks().get(new IdImpl(nextStop.getLinkId()));
				if(prevL.equals(nextL))
					bestPath = new Path(new ArrayList<Node>(), new ArrayList<Link>(), 0.0, 0.0);
				else {
					LeastCostPathCalculator leastCostPathCalculator = new AStarEuclidean(network, preProcessData, timeFunction);
					bestPath = leastCostPathCalculator.calcLeastCostPath(prevL.getToNode(), nextL.getFromNode(), 0);
					bestPath.links.add(0,prevL);
				}
				bestPath.links.add(nextL);
			}
			else {
				nextLL=getBestLinksMode(network,"Car", nextStop.getPoint(),trip.getShape());
				List<Tuple<Path,Link>> paths = new ArrayList<Tuple<Path,Link>>();
				for(int i=0; i<nextLL.size(); i++) {
					nextL = nextLL.get(i);
					Path path;
					if(prevL.equals(nextL))
						path = new Path(new ArrayList<Node>(), new ArrayList<Link>(), 0.0, 0.0);
					else {
						LeastCostPathCalculator leastCostPathCalculator = new AStarEuclidean(network, preProcessData, timeFunction);
						path = leastCostPathCalculator.calcLeastCostPath(prevL.getToNode(), nextL.getFromNode(), 0);
						path.links.add(0,prevL);
					}
					path.links.add(nextL);
					paths.add(new Tuple<Path,Link>(path,nextL));
				}	
				double shortestDistance = Double.POSITIVE_INFINITY;
				for(Tuple<Path,Link> tuple:paths) {
					double distance = calculateDistance(tuple.getFirst(),prevStop.getPoint(),nextStop.getPoint());
					if(bestPath==null||distance<=shortestDistance) {
						shortestDistance = distance;
					 	bestPath = tuple.getFirst();
					 	nextStop.setLinkId(tuple.getSecond().getId().toString());
					}
				}
			}
			prevL = bestPath.links.get(bestPath.links.size()-1);
			bestPath.links.remove(0);
			links.addAll(bestPath.links);
			prevStop = nextStop;
		}
	}
	private List<Link> getBestLinksMode(Network network, String mode, Coord coord, Shape shape) {
		List<Double> nearestDistances = new ArrayList<Double>();
		List<Link> nearestLinks = new ArrayList<Link>();
		for(double minDistance=this.minDistance;nearestLinks.size()<numCandidates;minDistance+=MIN_DISTANCE_DELTA)
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
	private double calculateDistance(Path path, Coord prevStop, Coord nextStop) {
		LinkImpl firstLink = (LinkImpl)path.links.get(0);
		Point2D fromPoint = new Point2D(firstLink.getFromNode().getCoord().getX(), firstLink.getFromNode().getCoord().getY());
		Point2D toPoint = new Point2D(firstLink.getToNode().getCoord().getX(), firstLink.getToNode().getCoord().getY());
		Line2D firstLinkLine = new Line2D(fromPoint, toPoint);
		double distance = firstLink.calcDistance(prevStop);
		if(path.links.size()==1) {
			distance += firstLinkLine.getNearestPoint(new Point2D(prevStop.getX(), prevStop.getY())).getDistance(firstLinkLine.getNearestPoint(new Point2D(nextStop.getX(), nextStop.getY())));
			distance += firstLink.calcDistance(nextStop);
		}
		else {
			Coord firstToNodeCoord = firstLink.getToNode().getCoord();
			distance += firstLinkLine.getNearestPoint(new Point2D(prevStop.getX(), prevStop.getY())).getDistance(new Point2D(firstToNodeCoord.getX(),firstToNodeCoord.getY()));
			LinkImpl lastLink = (LinkImpl)path.links.get(path.links.size()-1);
			Point2D fromPoint2 = new Point2D(lastLink.getFromNode().getCoord().getX(), lastLink.getFromNode().getCoord().getY());
			Point2D toPoint2 = new Point2D(lastLink.getToNode().getCoord().getX(), lastLink.getToNode().getCoord().getY());
			Line2D lastLinkLine = new Line2D(fromPoint2, toPoint2);
			for(int i=1; i<path.links.size()-1; i++)
				distance += path.links.get(i).getLength();
			Coord lastFromNodeCoord = lastLink.getFromNode().getCoord();
			distance += lastLinkLine.getNearestPoint(new Point2D(nextStop.getX(), nextStop.getY())).getDistance(new Point2D(lastFromNodeCoord.getX(),lastFromNodeCoord.getY()));
			distance += lastLink.calcDistance(nextStop);
		}
		return distance;
	}
	public int isPathJoined() {
		for(int i=0; i<links.size()-1; i++)
			if(!links.get(i).getToNode().equals(links.get(i+1).getFromNode()))
				return i;
		return -1;
	}
	public int isPathWithoutUs() {
		for(int i=1; i<links.size()-2; i++)
			if(links.get(i).getFromNode().equals(links.get(i+1).getToNode()))
				return i;
		return -1;
	}
	public int isPathWithoutRepeatedLink() {
		for(int i=0; i<links.size()-1; i++)
			for(int j=i+1; j<links.size(); j++)
				if(links.get(i).equals(links.get(j))&&!(i==0 && j==links.size()-1))
					return i;
		return -1;
	}
	public String allStopsWithLink() {
		for(StopTime stopTime: trip.getStopTimes().values())
			if(stops.get(stopTime.getStopId()).getLinkId()==null)
				return stopTime.getStopId();
		return "";
	}
	public int isFirstLinkWithStop() {
		String firstStopLink = stops.get(trip.getStopTimes().values().iterator().next().getStopId()).getLinkId();
		if(!firstStopLink.equals(links.get(0).getId().toString()))
			return getLinkPosition(firstStopLink)-1;
		return -1;
	}
	public String allStopsWithCorrectLink() {
		for(StopTime stopTime: trip.getStopTimes().values()) {
			Stop stop = stops.get(stopTime.getStopId());
			Link link = network.getLinks().get(new IdImpl(stop.getLinkId()));
			Point2D fromPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Point2D toPoint = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			Line2D linkLine = new Line2D(fromPoint, toPoint);
			Point2D point = new Point2D(stop.getPoint().getX(),stop.getPoint().getY());
			if(!linkLine.isNearestInside(point)) {
				int pos=getLinkPosition(link.getId().toString());
				if(pos==-1)
					return stopTime.getStopId();
				if(pos==links.size()-1)
					return "";
				Link link2 = links.get(pos+1);
				fromPoint = new Point2D(link2.getFromNode().getCoord().getX(), link2.getFromNode().getCoord().getY());
				toPoint = new Point2D(link2.getToNode().getCoord().getX(), link2.getToNode().getCoord().getY());
				Line2D linkLine2 = new Line2D(fromPoint, toPoint);
				if(!(linkLine.getPointPosition(point).equals(Line2D.PointPosition.AFTER)&&linkLine2.getPointPosition(point).equals(Line2D.PointPosition.BEFORE)))
					return stopTime.getStopId();
			}
		}
		return "";
	}
	public String allStopsWithInRouteLink() {
		for(StopTime stopTime: trip.getStopTimes().values()) {
			Stop stop = stops.get(stopTime.getStopId());
			Link link = network.getLinks().get(new IdImpl(stop.getLinkId()));
			if(!links.contains(link))
				return stopTime.getStopId();
		}
		return "";
	}
	private int getLinkPosition(String link) {
		for(int i=0; i<links.size(); i++)
			if(link.equals(links.get(i).getId().toString()))
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
	public boolean addLinkStop(int selectedLinkIndex, String selectedStopId) {
		return stops.get(selectedStopId).setLinkId(getLink(selectedLinkIndex).getId().toString());
	}
	public void forceAddLinkStop(int selectedLinkIndex, String selectedStopId) {
		stops.get(selectedStopId).forceSetLinkId(getLink(selectedLinkIndex).getId().toString());
	}
	public void removeLinkStop(String selectedStopId) {
		stops.get(selectedStopId).setLinkId(null);
	}
	public void setWithShapeCost() {
		TravelMinCost travelMinCost = null;
		if(withShapeCost) {
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
		else {
			travelMinCost = new TravelMinCost() {
				public double getLinkGeneralizedTravelCost(Link link, double time) {
					return getLinkMinimumTravelCost(link);
				}
				public double getLinkMinimumTravelCost(Link link) {
					return link.getLength()*Math.pow(trip.getShape().getDistance(link),1);
				}
			};
			preProcessData = new PreProcessEuclidean(travelMinCost);
			preProcessData.run(network);
		}
		withShapeCost = !withShapeCost;
	}
	
}
