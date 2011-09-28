package playground.gregor.sim2d_v2.helper.experimentalgraphgenerator;


public class SkeletonLink {

	private final SkeletonNode fromNode;
	private final SkeletonNode toNode;

	public SkeletonLink(SkeletonNode fromNode, SkeletonNode toNode) {
		this.fromNode = fromNode;
		this.toNode = toNode;
	}

	public SkeletonNode getFromNode() {
		return this.fromNode;
	}

	public SkeletonNode getToNode() {
		return this.toNode;
	}

	public double getLength() {
		return this.fromNode.getGeometry().distance(this.toNode.getGeometry());
	}

	public boolean isDeadEnd() {
		if (this.fromNode.getLinkedLinks().size() == 1 || this.toNode.getLinkedLinks().size() == 1) {
			return true;
		}
		return false;
	}

}
