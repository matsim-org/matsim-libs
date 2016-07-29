package gunnar.ihop2.transmodeler.networktransformation;

import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.newUnidirectionalLinkId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.DIR;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerLink extends TransmodelerElement {

	private final String bidirectionalId;

	private final DIR dir;

	private final TransmodelerNode fromNode;

	private final TransmodelerNode toNode;

	private final String type;

	final SortedSet<TransmodelerSegment> segments = new TreeSet<TransmodelerSegment>();

	final Map<TransmodelerLink, Double> downstreamLink2turnLength = new LinkedHashMap<TransmodelerLink, Double>();

	TransmodelerLink(final String bidirectionalId, final DIR dir,
			final String abDir, final String baDir,
			final TransmodelerNode fromNode, final TransmodelerNode toNode,
			final String type) {
		super(newUnidirectionalLinkId(bidirectionalId, dir, abDir, baDir));
		this.bidirectionalId = bidirectionalId;
		this.dir = dir;
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.type = type;
	}

	boolean segmentIsUpstream(final String unidirSegmentId) {
		if (DIR.AB.equals(this.dir)) {
			// the highest position index is upstream in direction AB
			return this.segments.last().getId().equals(unidirSegmentId);
		} else {
			// the lowest position index is upstream in direction BA
			return this.segments.first().getId().equals(unidirSegmentId);
		}
	}

	// TODO NEW, check assumptions with Olivier
	boolean segmentIsDownstream(final String unidirSegmentId) {
		if (DIR.AB.equals(this.dir)) {
			// the lowest position index is downstream in direction AB
			return this.segments.first().getId().equals(unidirSegmentId);
		} else {
			// the highest position index is downstream in direction BA
			return this.segments.last().getId().equals(unidirSegmentId);
		}
	}

	TransmodelerNode getFromNode() {
		return fromNode;
	}

	TransmodelerNode getToNode() {
		return toNode;
	}

	String getType() {
		return type;
	}

	String getBidirectionalId() {
		return this.bidirectionalId;
	}

	DIR getDirection() {
		return this.dir;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(id=" + this.getId()
				+ ", bidirectionalId = " + this.bidirectionalId
				+ ", direction=" + this.dir + ", fromNode="
				+ this.fromNode.getId() + ", toNode=" + this.toNode.getId()
				+ ", type=" + this.type + ")";
	}
}
