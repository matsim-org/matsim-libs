package ch.sbb.matsim.contrib.railsim.prototype.supply;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stop information
 * <p>
 * Can also be passes for some transit lines.
 *
 * @author Merlin Unterfinger
 */
public class StopInfo {

	private final String id;
	private final Coord coord;
	private final double stopLinkLength;
	private final Map<String, Object> linkAttributes = new HashMap<>();
	private TransitStopFacility stop;
	private Link stopLink;
	private DepotInfo depotInfo;

	/**
	 * @param id             the unique id or name of the stop.
	 * @param coord          the coordinates of the stop,
	 * @param stopLinkLength the maximum length of the stop link in the station.
	 */
	public StopInfo(String id, Coord coord, double stopLinkLength) {
		this.id = id;
		this.coord = coord;
		this.stopLinkLength = stopLinkLength;
		this.depotInfo = null;
	}

	/**
	 * Checks if the stopInfo has no associated depot.
	 *
	 * @return true if no depot is connected, false otherwise.
	 */
	boolean hasNoDepot() {
		return depotInfo == null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		var stopInfo = (StopInfo) o;
		// only compare id since uniqueness in managed by builder
		return Objects.equals(id, stopInfo.id);
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

	public String getId() {
		return id;
	}

	public Coord getCoord() {
		return coord;
	}

	public double getStopLinkLength() {
		return stopLinkLength;
	}

	public Map<String, Object> getLinkAttributes() {
		return linkAttributes;
	}

	public TransitStopFacility getStop() {
		return stop;
	}

	public void setStop(TransitStopFacility stop) {
		this.stop = stop;
	}

	public Link getStopLink() {
		return stopLink;
	}

	public void setStopLink(Link stopLink) {
		this.stopLink = stopLink;
	}

	public DepotInfo getDepotInfo() {
		return depotInfo;
	}

	public void setDepotInfo(DepotInfo depotInfo) {
		this.depotInfo = depotInfo;
	}
}
