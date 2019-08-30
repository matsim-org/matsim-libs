package org.matsim.contrib.accessibility.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public final class AggregationObject {
	
	private List<Id> objectIdList = null; // either job or person id
	private Id zoneID;
	private Id parcelID;
	private BasicLocation nearestNode;
	
	private double sum = 0. ;
	private double cnt = 0. ;
	
	// not used anywhere, dz, jul'17
//	public AggregationObject(Id objectID, Id parcelId, Id zoneId, double value){
//		this(objectID, parcelId, zoneId, null, value);
//	}
	
	public AggregationObject(Id objectID, Id parcelId, Id zoneId, BasicLocation nearestNode, double value){
		if(this.objectIdList == null)
			this.objectIdList = new ArrayList<Id>();
		this.objectIdList.add( objectID );
		this.parcelID = parcelId;
		this.zoneID = zoneId;
		this.nearestNode = nearestNode;
		
		this.sum = value;
	}
	
	public void setNearestNode(Node nearestNode){
		this.nearestNode = nearestNode;
	}
	
	public void addObject(Id objectID, double value){
		this.objectIdList.add( objectID );
		this.sum += value;
		this.cnt ++ ; // could be generalized into a weight.  kai, mar'14
	}
	
	public BasicLocation getNearestNode(){
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
	
	public double getSum(){
		return this.sum;
	}
	public double getCnt() {
		return this.cnt ;
	}
}
