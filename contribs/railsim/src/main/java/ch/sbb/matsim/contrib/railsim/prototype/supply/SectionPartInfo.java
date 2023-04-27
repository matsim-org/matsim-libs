package ch.sbb.matsim.contrib.railsim.prototype.supply;

import java.util.ArrayList;
import java.util.List;

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
