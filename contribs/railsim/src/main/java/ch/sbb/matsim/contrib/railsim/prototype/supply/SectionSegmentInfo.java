package ch.sbb.matsim.contrib.railsim.prototype.supply;

import org.matsim.api.core.v01.Coord;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		var that = (SectionSegmentInfo) o;
		if (Double.compare(that.length, length) != 0) return false;
		if (!Objects.equals(fromCoord, that.fromCoord)) return false;
		if (!Objects.equals(toCoord, that.toCoord)) return false;
		return linkAttributes.equals(that.linkAttributes);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = fromCoord != null ? fromCoord.hashCode() : 0;
		result = 31 * result + (toCoord != null ? toCoord.hashCode() : 0);
		temp = Double.doubleToLongBits(length);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
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
