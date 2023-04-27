package ch.sbb.matsim.contrib.railsim.prototype.supply;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.HashMap;
import java.util.Map;

/**
 * Depot information
 *
 * @author Merlin Unterfinger
 */
public class DepotInfo {
	private final String id;
	private final Coord coord;
	private final double length;
	private final double inLength;
	private final double outLength;
	private final int capacity;
	private final Map<String, Object> inLinkAttributes = new HashMap<>();
	private final Map<String, Object> depotLinkAttributes = new HashMap<>();
	private final Map<String, Object> outLinkAttributes = new HashMap<>();
	private TransitStopFacility depot;
	private Link depotIn;
	private Link depotLink;
	private Link depotOut;

	public DepotInfo(String id, Coord coord, double length, double inLength, double outLength, int capacity) {
		this.id = id;
		this.coord = coord;
		this.length = length;
		this.capacity = capacity;
		this.inLength = inLength;
		this.outLength = outLength;
	}

	public Map<String, Object> getInLinkAttributes() {
		return inLinkAttributes;
	}

	public Map<String, Object> getDepotLinkAttributes() {
		return depotLinkAttributes;
	}

	public Map<String, Object> getOutLinkAttributes() {
		return outLinkAttributes;
	}

	public String getId() {
		return id;
	}

	public Coord getCoord() {
		return coord;
	}

	public double getLength() {
		return length;
	}

	public int getCapacity() {
		return capacity;
	}

	public double getInLength() {
		return inLength;
	}

	public double getOutLength() {
		return outLength;
	}

	public TransitStopFacility getDepot() {
		return depot;
	}

	public Link getDepotIn() {
		return depotIn;
	}

	public Link getDepotLink() {
		return depotLink;
	}

	public Link getDepotOut() {
		return depotOut;
	}

	public void setDepot(TransitStopFacility depot) {
		this.depot = depot;
	}

	public void setDepotIn(Link depotIn) {
		this.depotIn = depotIn;
	}

	public void setDepotLink(Link depotLink) {
		this.depotLink = depotLink;
	}

	public void setDepotOut(Link depotOut) {
		this.depotOut = depotOut;
	}
}
