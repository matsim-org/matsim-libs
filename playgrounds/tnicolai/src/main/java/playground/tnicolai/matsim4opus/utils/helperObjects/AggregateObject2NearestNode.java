package playground.tnicolai.matsim4opus.utils.helperObjects;

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
	
	private double sumWalkDistanceCost;
	private double sumWalkPowerDistanceCost;
	private double sumWalkLnDistanceCost;
	
	private double sumWalkTravelTimeCost;
	private double sumWalkPowerTravelTimeCost;
	private double sumWalkLnTravelTimeCost;
	
	private double sumWalkMonetaryTravelCost;
	private double sumWalkPowerMonetaryTravelCost;
	private double sumWalkLnMonetaryTravelCost;
	
	public AggregateObject2NearestNode(Id objectID, Id parcelId, Id zoneId, Coord coordinate, 
										double walkDistanceCost, double walkPowerDistanceCost, double walkLnDistanceCost,
										double walkTravelTimeCost, double walkPowerTravelTimeCost, double walkLnTravelTimeCost,
										double walkMonetaryTravelCost, double walkPowerMonetaryTravelCost, double walkLnMonetaryTravelCost){
		this(objectID, parcelId, zoneId, coordinate, null, 
				walkDistanceCost, walkPowerDistanceCost, walkLnDistanceCost,
				walkTravelTimeCost, walkPowerTravelTimeCost, walkLnTravelTimeCost,
				walkMonetaryTravelCost, walkPowerMonetaryTravelCost, walkLnMonetaryTravelCost);
	}
	
	public AggregateObject2NearestNode(Id objectID, Id parcelId, Id zoneId, Coord coordinate, Node nearestNode, 
										double walkDistanceCost, double walkPowerDistanceCost, double walkLnDistanceCost,
										double walkTravelTimeCost, double walkPowerTravelTimeCost, double walkLnTravelTimeCost,
										double walkMonetaryTravelCost, double walkPowerMonetaryTravelCost, double walkLnMonetaryTravelCost){
		if(this.objectIdList == null)
			this.objectIdList = new ArrayList<Id>();
		this.objectIdList.add( objectID );
		this.parcelID = parcelId;
		this.zoneID = zoneId;
		this.coordinate = coordinate;
		this.nearestNode = nearestNode;
		this.distanceSum = 0.;
		
		this.sumWalkDistanceCost 	= walkDistanceCost;
		this.sumWalkPowerDistanceCost = walkPowerDistanceCost;
		this.sumWalkLnDistanceCost 	= walkLnDistanceCost;
		
		this.sumWalkTravelTimeCost	= walkTravelTimeCost;
		this.sumWalkPowerTravelTimeCost = walkPowerTravelTimeCost;
		this.sumWalkLnTravelTimeCost= walkLnTravelTimeCost;
		
		this.sumWalkMonetaryTravelCost = walkMonetaryTravelCost;
		this.sumWalkPowerMonetaryTravelCost = walkPowerMonetaryTravelCost;
		this.sumWalkLnMonetaryTravelCost = walkLnMonetaryTravelCost;
	}
	
	@Deprecated
	public AggregateObject2NearestNode(Id objectID, Id parcelId, Id zoneId, Coord coordinate, double distance){
		this(objectID, parcelId, zoneId, coordinate, null, distance);
	}
	@Deprecated
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
	
	@Deprecated
	public void addObject(Id objectID, double distance){
		this.objectIdList.add( objectID );
		this.distanceSum += distance;
	}
	
	public void addObject(Id objectID, 
							double walkDistanceCost, double walkPowerDistanceCost, double walkLnDistanceCost,
							double walkTravelTimeCost, double walkPowerTravelTimeCost, double walkLnTravelTimeCost,
							double walkMonetaryTravelCost, double walkPowerMonetaryTravelCost, double walkLnMonetaryTravelCost){
		this.objectIdList.add( objectID );
		this.sumWalkDistanceCost 	+= walkDistanceCost;
		this.sumWalkPowerDistanceCost += walkPowerDistanceCost;
		this.sumWalkLnDistanceCost 	+= walkLnDistanceCost;
		
		this.sumWalkTravelTimeCost	+= walkTravelTimeCost;
		this.sumWalkPowerTravelTimeCost += walkPowerTravelTimeCost;
		this.sumWalkLnTravelTimeCost+= walkLnTravelTimeCost;
		
		this.sumWalkMonetaryTravelCost += walkMonetaryTravelCost;
		this.sumWalkPowerMonetaryTravelCost += walkPowerMonetaryTravelCost;
		this.sumWalkLnMonetaryTravelCost += walkLnMonetaryTravelCost;
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
	
	public double getSumWalkDistanceCost(){
		return this.sumWalkDistanceCost;		
	}
	
	public double getSumWalkPowerDistanceCost(){
		return this.sumWalkPowerDistanceCost;
	}
	
	public double getSumWalkLnDistanceCost(){
		return this.sumWalkLnDistanceCost;
	}
	
	public double getSumWalkTravelTimeCost(){
		return this.sumWalkTravelTimeCost;
	}
	
	public double getSumWalkPowerTravelTimeCost(){
		return this.sumWalkPowerTravelTimeCost;
	}
	
	public double getSumWalkLnTravelTimeCost(){
		return this.sumWalkLnTravelTimeCost;
	}
	
	public double getSumWalkMonetaryTravelCost(){
		return this.sumWalkMonetaryTravelCost;
	}
	
	public double getSumWalkPowerMonetaryTravelCost(){
		return this.sumWalkPowerMonetaryTravelCost;
	}
	
	public double getSumWalkLnMonetaryTravelCost(){
		return this.sumWalkLnMonetaryTravelCost;
	}
	
	@Deprecated
	public double getAverageDistance(){
		if(this.objectIdList != null &&
		   this.objectIdList.size() > 0)
			return this.distanceSum / this.objectIdList.size();
		return 0.;			
	}
}
