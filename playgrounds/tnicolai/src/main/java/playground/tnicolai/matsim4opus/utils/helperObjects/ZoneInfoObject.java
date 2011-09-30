package playground.tnicolai.matsim4opus.utils.helperObjects;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public class ZoneInfoObject {
	
	private Coord zoneCoodrdinate;
	private Node nearestNode;
	private Id zoneID;

	/**
	 * constructor
	 * @param zoneCoodrdinate
	 * @param nearestNode
	 * @param zone
	 */
	public ZoneInfoObject(Id zoneID, Coord zoneCoodrdinate, Node nearestNode){
		this.zoneID = zoneID;
		this.nearestNode = nearestNode;
		this.zoneCoodrdinate = zoneCoodrdinate;		
	}
	
	public Coord getZoneCoordinate(){
		return this.zoneCoodrdinate;
	}
	public Id getZoneID(){
		return this.zoneID;
	}
	public Node getNearestNode(){
		return this.nearestNode;
	}
	
}
