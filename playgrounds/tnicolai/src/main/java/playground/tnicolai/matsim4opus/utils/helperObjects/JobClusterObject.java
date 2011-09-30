package playground.tnicolai.matsim4opus.utils.helperObjects;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public class JobClusterObject {
	
	private List<Id> jobIdList;
	private Id zoneID;
	private Coord coordinate;
	private Node nearestNode;
	
	public JobClusterObject(Id jobID, Id zoneId, Coord coordinate){
		this.jobIdList = new ArrayList<Id>();
		this.jobIdList.add( jobID );
		this.zoneID = zoneId;
		this.coordinate = coordinate;
		this.nearestNode = null;
	}
	
	public void addNearestNode(Node nearestNode){
		this.nearestNode = nearestNode;
	}
	
	public void addJob(Id jobID){
		this.jobIdList.add( jobID );
	}
	
	public Node getNearestNode(){
		return this.nearestNode;
	}
	
	public int getNumberOfJobs(){
		return this.jobIdList.size();
	}
	
	public List<Id> getJobIds(){
		return this.jobIdList;
	}
	
	public Id getZoneID(){
		return this.zoneID;
	}
	
	public Coord getCoordinate(){
		return this.coordinate;
	}

}
