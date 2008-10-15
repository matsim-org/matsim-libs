package playground.mmoyo.PTRouter;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.IdImpl;

/** 
 * Describes a Public Transport Network Line
 * @param id  Unique Identifier 
 * @param type Describes if the line is Bus, Trolley, Subway. etc.
 * @param withDedicatedTracks Points out if the line has own rails or interacts with normal city traffic
 */
public class PTLine {
	private IdImpl id;
	private PTLineType ptLineType;
	private String direction;
	private List<String> route = new ArrayList<String>();  //List of nodes
	private List<Double> minute = new ArrayList<Double>();
	private List<String> departures = new ArrayList<String>();

	public PTLine(IdImpl id, char lineType, List<String> route) {
		this.id = id;
		this.ptLineType = new PTLineType(lineType);
		this.route = route;
	}

	public PTLine(IdImpl id, char lineType, String direction, List<String> route, List<Double> minute, List<String> departures) {
		this.id = id;
		this.ptLineType = new PTLineType(lineType);
		this.route = route;
		this.minute = minute;
		this.direction = direction;
		this.departures=departures;
	}
		
	public IdImpl getId() {
		return id;
	}

	public void setId(IdImpl id) {
		this.id = id;
	}

	public PTLineType getPtLineType() {
		return ptLineType;
	}

	public void setPtLineType(PTLineType ptLineType) {
		this.ptLineType = ptLineType;
	}

	public List<String> getRoute() {
		return route;
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

}// class
