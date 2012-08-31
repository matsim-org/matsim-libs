package playground.andreas.aas.modules.cellBasedAccessibility.utils.helperObjects;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public class AggregateObject2NearestNode {
	
	private List<Id> objectIdList = null; // either job or person id
	private Id zoneID;
	private Id parcelID;
	private Coord coordinate;
	private Node nearestNode;
	private double distanceSum;
	
	public AggregateObject2NearestNode(Id objectID, Id parcelId, Id zoneId, Coord coordinate, double distance){
		if(this.objectIdList == null)
			this.objectIdList = new ArrayList<Id>();
		this.objectIdList.add( objectID );
		this.parcelID = parcelId;
		this.zoneID = zoneId;
		this.coordinate = coordinate;
		this.nearestNode = null;
		this.distanceSum = distance;
	}
	
	public AggregateObject2NearestNode(Id objectID, Id parcelId, Id zoneId, Coord coordinate, Node nearestNode, double distance){
		if(this.objectIdList == null)
			this.objectIdList = new ArrayList<Id>();
		this.objectIdList.add( objectID );
		this.parcelID = parcelId;
		this.zoneID = zoneId;
		this.coordinate = coordinate;
		this.nearestNode = nearestNode;
		this.distanceSum = distance;
	}
	
	public void setNearestNode(Node nearestNode){
		this.nearestNode = nearestNode;
	}
	
	public void addObject(Id objectID, double distance){
		this.objectIdList.add( objectID );
		this.distanceSum += distance;
	}
	
	public Node getNearestNode(){
		return this.nearestNode;
	}
	
	public int getNumberOfObjects(){
		return this.objectIdList.size();
	}
	
	public List<Id> getObjectIds(){
		return this.objectIdList;
	}
	
	public Id getParcelID(){
		return this.parcelID;
	}
	
	public Id getZoneID(){
		return this.zoneID;
	}
	
	public Coord getCoordinate(){
		return this.coordinate;
	}
	
	public double getAverageDistance(){
		if(this.objectIdList != null &&
		   this.objectIdList.size() > 0)
			return this.distanceSum / this.objectIdList.size();
		return 0.;			
	}

}
