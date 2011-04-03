package playground.sergioo.PathEditor.kernel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.PreProcessEuclidean;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;

import playground.sergioo.GTFS.Trip;

public class RoutePath {
	
	//Constants
	private static final double MIN_DISTANCE_DELTA = 50*180/(6371000*Math.PI);
	
	//Attributes
	public List<LinkImpl> links;
	private final Network network;
	private final Trip trip;
	//Parameters
	private double minDistance = 300*180/(6371000*Math.PI);
	private int numOptions = 6;
	private boolean withAngleShape = false;
	private boolean withCostShape = false;

	private PreProcessEuclidean preProcessData;
	
	//Methods
	public RoutePath(Network network, Trip trip) {
		super();
		this.network = network;
		this.trip = trip;
		links = new ArrayList<LinkImpl>();
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
	}
	public List<LinkImpl> getLinks() {
		return links;
	}
	public LinkImpl getLink(int index) {
		return links.get(index);
	}
	public int getIndexNearestLink(Coord coord) {
		int nearest = -1;
		double nearestDistance = Double.POSITIVE_INFINITY;
		for(int i=0; i<links.size(); i++) {
			double distance = links.get(i).calcDistance(coord); 
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
	public void addLink(int index, double angle) {
		//TODO
	}
	public void addShortestPath(int indexI, int indexF) {
		//TODO
	}
	public void calculatePath() {
		//TODO
	}
	public boolean isPathJoined() {
		Iterator<LinkImpl> linkI = links.iterator();
		LinkImpl link = linkI.next();
		while(linkI.hasNext())
			if(!link.getToNode().equals(linkI.next().getFromNode()))
				return false;
		return true;
	}
	public void increaseMinDistance() {
		minDistance += MIN_DISTANCE_DELTA;
	}
	public void decreaseMinDistance() {
		if(minDistance-MIN_DISTANCE_DELTA>0)
			minDistance -= MIN_DISTANCE_DELTA;
	}
	public void increaseNumOptions() {
		numOptions ++;
	}
	public void decreaseNumOptions() {
		if(numOptions-1>0)
			numOptions --;
	}
	public void setWithAngleShape(boolean withAngleShape) {
		this.withAngleShape = withAngleShape;
	}
	public void setWithCostShape(boolean withCostShape) {
		TravelMinCost travelMinCost = null;
		if(this.withCostShape && !withCostShape) {
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
			this.withCostShape = withCostShape;
		}
		else if(!this.withCostShape && withCostShape) {
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
			this.withCostShape = withCostShape;
		}
	}
	
}
