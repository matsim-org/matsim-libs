package playground.mmoyo.PTRouter;

import java.util.ArrayList;
import java.util.List;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;

/** 
 * Representation of a PT Line containing the sequence of stations and departures for every one of them
 * @param id  Unique Identifier 
 * @param type Describes if the line is Bus, Trolley, Subway. etc.
 * @param withDedicatedTracks Points out if the line has own rails or interacts with normal city traffic
 */
public class PTLine {
	private Id id;
	private TransportMode transportMode ;
	private String direction;
	private List<Id> nodeRoute = new ArrayList<Id>();      /**List of sequential nodes Id*/
	private List<Double> minute = new ArrayList<Double>();     /**average arrival time to each station after departure from first station*/
	private List<String> departures = new ArrayList<String>(); /**Departures from first stations during the day in format "hh:mm" */

	public PTLine(Id id, char lineType, List<Id> nodeRoute) {
		this.id = id;
		//this.ptLineType = new PTLineType(lineType);
		this.nodeRoute = nodeRoute;
	}

	public PTLine(Id id, TransportMode transportMode, String direction, List<Id> nodeRoute, List<Double> minute, List<String> departures) {
		this.id = id;
		this.transportMode = transportMode;
		this.nodeRoute = nodeRoute;
		this.minute = minute;
		this.direction = direction;
		this.departures=departures;
	}
		
	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	/*
	public PTLineType getPtLineType() {
		return ptLineType;
	}

	public void setPtLineType(PTLineType ptLineType) {
		this.ptLineType = ptLineType;
	}
	*/

	public List<Id> getNodeRoute() {
		return nodeRoute;
	}

	public String getDirection() {
		return direction;
	}

	public List<Double> getMinutes() {
		return minute;
	}

	public List<String> getDepartures() {
		return departures;
	}

	public TransportMode getTransportMode() {
		return transportMode;
	}

	public void setTransportMode(TransportMode transportMode) {
		this.transportMode = transportMode;
	}
}