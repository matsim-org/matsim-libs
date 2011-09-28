package playground.gregor.sim2d_v2.helper.experimentalgraphgenerator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class SkeletonLinksContraction {

	private static final double MIN_DIST = 0.25;

	private static double CONTRACTION_THRESHOLD = 1.5;

	private final GeometryFactory geofac = new GeometryFactory();


	private Queue<SkeletonLink> links;
	private Set<SkeletonLink> killed;

	private Skeleton skeleton;

	private Geometry bounds;

	public void contractShortSkeletonLinks(Skeleton skeleton, Geometry bounds) {

		this.links = new LinkedList<SkeletonLink>(skeleton.getLinks());
		this.killed = new LinkedHashSet<SkeletonLink>();
		this.skeleton = skeleton;
		this.bounds = bounds;

		while (this.links.size() > 0) {
			SkeletonLink link = this.links.poll();
			if (this.killed.contains(link) || link.isDeadEnd()) {
				continue;
			}
			if (link.getLength() < CONTRACTION_THRESHOLD){
				tryToContract(link);
			}
		}
	}

	private void tryToContract(SkeletonLink link) {


		List<SkeletonLink> rmLinks = new ArrayList<SkeletonLink>();
		rmLinks.add(link);

		double x = (link.getFromNode().getCoord().x+link.getToNode().getCoord().x)/2;
		double y = (link.getFromNode().getCoord().y+link.getToNode().getCoord().y)/2;
		Coordinate c = new Coordinate(x,y);

		List<SkeletonNode> others = new ArrayList<SkeletonNode>();

		for (SkeletonLink l : link.getFromNode().getLinkedLinks()) {
			if (l.equals(link)) {
				continue;
			}
			rmLinks.add(l);
			if (!l.getFromNode().equals(link.getFromNode())) {
				others.add(l.getFromNode());
			} else {
				others.add(l.getToNode());
			}
		}

		for (SkeletonLink l : link.getToNode().getLinkedLinks()) {
			if (l.equals(link)) {
				continue;
			}
			rmLinks.add(l);
			if (!l.getFromNode().equals(link.getToNode())) {
				others.add(l.getFromNode());
			} else {
				others.add(l.getToNode());
			}
		}

		if (!intersecting(others,c)) {
			for (SkeletonLink rmLink : rmLinks) {
				this.skeleton.removeLink(rmLink);
				this.killed.add(rmLink);
			}
			this.skeleton.removeNode(link.getFromNode());
			this.skeleton.removeNode(link.getToNode());
			SkeletonNode node = this.skeleton.getOrCreateNode(this.geofac.createPoint(c));
			for (SkeletonNode other : others) {
				SkeletonLink otherLink = this.skeleton.createAndAddLink(other, node);
				this.links.add(otherLink);
			}
		}

	}

	private boolean intersecting(List<SkeletonNode> others, Coordinate c) {

		for (SkeletonNode other : others) {
			LineString ls = this.geofac.createLineString(new Coordinate[]{other.getCoord(),c});
			if (ls.intersects(this.bounds) || other.getLinkedLinks().size() > 1 && ls.distance(this.bounds) < MIN_DIST) {
				return true;
			}
		}

		return false;
	}

}
