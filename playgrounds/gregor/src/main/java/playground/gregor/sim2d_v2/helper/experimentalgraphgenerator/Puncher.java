package playground.gregor.sim2d_v2.helper.experimentalgraphgenerator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.vividsolutions.jts.geom.Envelope;

public class Puncher {

	public void punchSkeleton(Skeleton skeleton, Envelope boundary) {
		Set<SkeletonLink> links = new LinkedHashSet<SkeletonLink>();
		for (SkeletonLink link : skeleton.getLinks()) {
			if (!boundary.contains(link.getFromNode().getCoord()) && !boundary.contains(link.getToNode().getCoord())) {
				links.add(link);
			}
		}
		for (SkeletonLink link : links) {
			skeleton.removeLink(link);
		}
		List<SkeletonNode> nodes = new ArrayList<SkeletonNode>();
		for (SkeletonNode node : skeleton.getNodes()) {
			if (node.getLinkedLinks().size() == 0) {
				nodes.add(node);
			}
		}

		for (SkeletonNode node : nodes) {
			skeleton.removeNode(node);
		}
	}

}
