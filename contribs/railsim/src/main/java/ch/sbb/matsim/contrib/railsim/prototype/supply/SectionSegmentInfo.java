package ch.sbb.matsim.contrib.railsim.prototype.supply;

import org.matsim.api.core.v01.Coord;

import java.util.HashMap;
import java.util.Map;

/**
 * Section segment information
 * <p>
 * A section segment is a segment of the rail network with constant attributes (number of rails, speed limit, grade).
 *
 * @author Merlin Unterfinger
 */
public class SectionSegmentInfo {
	private final Coord fromCoord;
	private final Coord toCoord;
	private final double length;
	private final Map<String, Object> linkAttributes = new HashMap<>();

	public SectionSegmentInfo(Coord fromCoord, Coord toCoord, double length) {
		this.fromCoord = fromCoord;
		this.toCoord = toCoord;
		this.length = length;
	}

	public void addLinkAttribute(String key, Object value) {
		linkAttributes.put(key, value);
	}

	public Coord getFromCoord() {
		return fromCoord;
	}

	public Coord getToCoord() {
		return toCoord;
	}

	public double getLength() {
		return length;
	}

	public Map<String, Object> getLinkAttributes() {
		return linkAttributes;
	}
}
