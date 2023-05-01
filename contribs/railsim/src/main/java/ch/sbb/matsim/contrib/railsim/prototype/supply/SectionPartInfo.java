package ch.sbb.matsim.contrib.railsim.prototype.supply;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Section part information
 * <p>
 * A section part is a route between two stops, which is can be shared between multiple transit lines. Which means the rail capacity is also shared.
 *
 * @author Merlin Unterfinger
 */
public class SectionPartInfo {
	private final String fromStopId;
	private final String toStopId;
	private final List<SectionSegmentInfo> sectionSegmentInfos = new ArrayList<>();

	public SectionPartInfo(String fromStopId, String toStopId) {
		this.fromStopId = fromStopId;
		this.toStopId = toStopId;
	}

	public void addSegment(SectionSegmentInfo sectionSegmentInfo) {
		sectionSegmentInfos.add(sectionSegmentInfo);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		var that = (SectionPartInfo) o;
		if (!Objects.equals(fromStopId, that.fromStopId)) return false;
		if (!Objects.equals(toStopId, that.toStopId)) return false;
		return sectionSegmentInfos.equals(that.sectionSegmentInfos);
	}

	@Override
	public int hashCode() {
		int result = fromStopId != null ? fromStopId.hashCode() : 0;
		result = 31 * result + (toStopId != null ? toStopId.hashCode() : 0);
		return result;
	}

	public String getFromStopId() {
		return fromStopId;
	}

	public String getToStopId() {
		return toStopId;
	}

	public List<SectionSegmentInfo> getSectionSegmentInfos() {
		return sectionSegmentInfos;
	}

}
