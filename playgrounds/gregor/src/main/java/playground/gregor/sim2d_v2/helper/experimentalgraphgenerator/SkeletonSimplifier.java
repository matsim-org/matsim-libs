package playground.gregor.sim2d_v2.helper.experimentalgraphgenerator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class SkeletonSimplifier {

	private final Set<SkeletonLink> handledL = new LinkedHashSet<SkeletonLink>();


	List<SkeletonLink> rmLinks = new ArrayList<SkeletonLink>();
	List<SkeletonNode> rmNodes = new ArrayList<SkeletonNode>();

	List<Path> newPaths = new ArrayList<Path>();

	private final GeometryFactory geofac = new GeometryFactory();

	private Geometry bounds;

	public void simplifySkeleton(Skeleton skeleton, Geometry bounds) {
		this.bounds = bounds;
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
			if (p.isMultiHopPath() && !intersectsBounds(p.from, p.to)) {
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
		SkeletonNode next = null;
		if (l.getToNode().equals(from)) {
			next = l.getFromNode();
		} else {
			next = l.getToNode();
		}

		p.links.add(l);
		this.handledL.add(l);
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


	private boolean intersectsBounds(SkeletonNode from, SkeletonNode next) {
		LineString ls = this.geofac.createLineString(new Coordinate[] {from.getCoord(),next.getCoord()});
		if (ls.intersects(this.bounds)) {
			return true;
		}
		return false;
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
