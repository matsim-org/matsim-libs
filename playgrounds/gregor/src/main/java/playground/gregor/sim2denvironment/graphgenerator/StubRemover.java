package playground.gregor.sim2denvironment.graphgenerator;

import java.util.ArrayList;
import java.util.List;

public class StubRemover {

	private static final double MAX_STUB_LENGTH = 1.;

	public void run(Skeleton skeleton) {
		List<SkeletonLink> rmLinks = new ArrayList<SkeletonLink>();
		for (SkeletonLink link : skeleton.getLinks()) {
			if (link.isDeadEnd() && link.getLength() <= MAX_STUB_LENGTH) {
				rmLinks.add(link);
			}
		}

		List<SkeletonNode> rmNodes = new ArrayList<SkeletonNode>();
		for (SkeletonLink link : rmLinks) {
			SkeletonNode deadEndNode = link.getDeadEndNode();
			rmNodes.add(deadEndNode);
			skeleton.removeLink(link);
		}

		for (SkeletonNode node : rmNodes) {
			skeleton.removeNode(node);
		}
	}

}
