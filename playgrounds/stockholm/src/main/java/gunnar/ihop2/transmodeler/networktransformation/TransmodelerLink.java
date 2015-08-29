package gunnar.ihop2.transmodeler.networktransformation;

import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.newUnidirectionalLinkId;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerLink extends TransmodelerElement {

	private final String bidirectionalId;

	private final String direction;

	private final TransmodelerNode fromNode;

	private final TransmodelerNode toNode;

	private final String type;

	private final String pathIdPrefix;

	TransmodelerLink(final String bidirectionalId, final String direction,
			final TransmodelerNode fromNode, final TransmodelerNode toNode,
			final String type, final String pathIdPrefix) {
		super(newUnidirectionalLinkId(bidirectionalId, direction));
		this.bidirectionalId = bidirectionalId;
		this.direction = direction;
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.type = type;
		this.pathIdPrefix = pathIdPrefix;
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

	String getPathIdPrefix() {
		return this.pathIdPrefix;
	}

	String getPathId() {
		return this.pathIdPrefix + this.bidirectionalId;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(id=" + this.getId()
				+ ", bidirectionalId = " + this.bidirectionalId
				+ ", direction=" + this.direction + ", fromNode="
				+ this.fromNode.getId() + ", toNode=" + this.toNode.getId()
				+ ", type=" + this.type + ", pathId=" + this.getPathId() + ")";
	}
}
