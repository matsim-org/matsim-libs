package playground.gregor.sim2d_v2.experimental;


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

}
