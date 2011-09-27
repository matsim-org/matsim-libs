package playground.gregor.sim2d_v2.experimental;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkeletonSimplifier {

	private final Set<SkeletonLink> handledL = new HashSet<SkeletonLink>();

	List<SkeletonLink> rmLinks = new ArrayList<SkeletonLink>();
	List<SkeletonNode> rmNodes = new ArrayList<SkeletonNode>();

	List<Path> newPaths = new ArrayList<Path>();

	public void simplifySkeleton(Skeleton skeleton) {
		List<SkeletonNode> nodes = skeleton.getIntersectionAndDeadEndNodes();

		for (SkeletonNode node : nodes) {
			constructPaths(node);
		}

		for (SkeletonLink l : this.rmLinks) {
			skeleton.removeLink(l);
		}

		for (SkeletonNode n : this.rmNodes) {
			skeleton.removeNode(n);
		}

		for (Path p : this.newPaths) {
			skeleton.createAndAddLink(p.from, p.to);
		}

	}


	private void constructPaths(SkeletonNode node) {
		for (SkeletonLink l : node.getLinkedLinks()) {
			if (this.handledL.contains(l)) {
				continue;
			}

			Path p = new Path();
			p.from = node;
			constructPath(p,l,node);
			if (p.isMultiHopPath()) {
				for (SkeletonLink tmp : p.links) {
					this.rmLinks.add(tmp);
				}
				for (SkeletonNode tmp : p.nodes) {
					this.rmNodes.add(tmp);
				}
				this.newPaths.add(p);
			}

		}

	}


	private void constructPath(Path p, SkeletonLink l, SkeletonNode from) {
		p.links.add(l);
		this.handledL.add(l);
		SkeletonNode next = null;
		if (l.getToNode().equals(from)) {
			next = l.getFromNode();
		} else {
			next = l.getToNode();
		}
		if (next.isIntersectionOrDeadEnd()) {
			p.to = next;
		} else {
			p.nodes.add(next);
			for (SkeletonLink nextL : next.getLinkedLinks()) {
				if (!nextL.equals(l)) {
					constructPath(p,nextL,next);
					return;
				}
			}
		}

	}


	private static final class Path {

		List<SkeletonLink> links = new ArrayList<SkeletonLink>();
		List<SkeletonNode> nodes = new ArrayList<SkeletonNode>();
		SkeletonNode from;
		SkeletonNode to;


		public boolean isMultiHopPath() {
			return this.links.size() > 1;
		}


	}

}
