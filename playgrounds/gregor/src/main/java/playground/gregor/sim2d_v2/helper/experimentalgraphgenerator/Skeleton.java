package playground.gregor.sim2d_v2.helper.experimentalgraphgenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v2.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class Skeleton {

	private final Set<SkeletonLink> links = new LinkedHashSet<SkeletonLink>();
	private final QuadTree<SkeletonNode> nodes;

	private static final double TOLERANCE = 0.000000001;


	private int nodeNums = 0;

	public Skeleton(Envelope bounds) {
		this.nodes = new QuadTree<SkeletonNode>(bounds.getMinX(),bounds.getMinY(),bounds.getMaxX(),bounds.getMaxY());
	}



	public SkeletonNode getOrCreateNode(Point from) {
		Collection<SkeletonNode> neighbors = this.nodes.get(from.getX(),from.getY(),TOLERANCE);

		//TODO this can never happen?!
		if (neighbors.size() > 1) {
			throw new RuntimeException("There exists already a node at a different location with distance <= " + TOLERANCE + ". This should not happen!");
		}

		if (neighbors.size() == 1) {
			return neighbors.iterator().next();
		}

		SkeletonNode ret = new SkeletonNode(from, new IdImpl(this.nodeNums++));
		addNode(ret);
		return ret;
	}



	private void addNode(SkeletonNode ret) {
		this.nodes.put(ret.getCoord().x, ret.getCoord().y, ret);
	}



	//DEBUG
	public void dumpLinks(String file) {
		GeometryFactory geofac = new GeometryFactory();
		for (SkeletonLink link : this.links) {
			SkeletonNode from = link.getFromNode();
			SkeletonNode to = link.getToNode();
			Coordinate fromC = from.getCoord();
			Coordinate toC = to.getCoord();
			LineString ls = geofac.createLineString(new Coordinate[]{fromC, toC});
			GisDebugger.addGeometry(ls);
		}
		GisDebugger.dump(file);
	}
	//DEBUG
	public void dumpIntersectingNodes(String file) {
		for (SkeletonNode node : this.nodes.values()) {
			if (node.isIntersectionOrDeadEnd()) {
				GisDebugger.addGeometry(node.getGeometry());
			}
		}
		GisDebugger.dump(file);

	}


	public List<SkeletonNode> getIntersectionAndDeadEndNodes() {
		List<SkeletonNode> ret = new ArrayList<SkeletonNode>();
		for (SkeletonNode n: this.nodes.values()) {
			if (n.isIntersectionOrDeadEnd()) {
				ret.add(n);
			}
		}
		return ret;
	}



	public SkeletonLink createAndAddLink(SkeletonNode fromNode,
			SkeletonNode toNode) {
		SkeletonLink l  = new SkeletonLink(fromNode,toNode);
		fromNode.linkLink(l);
		toNode.linkLink(l);
		this.links.add(l);
		return l;
	}

	public boolean removeLink(SkeletonLink link) {
		link.getFromNode().removeLink(link);
		link.getToNode().removeLink(link);
		return this.links.remove(link);
	}

	public boolean removeNode(SkeletonNode node) {
		if (node.getLinkedLinks().size() > 0) {
			throw new RuntimeException("Can not remove node until all of its linked links have been removed");
		}
		return this.nodes.remove(node.getCoord().x, node.getCoord().y, node);
	}



	public Collection<SkeletonNode> getNodes() {
		return this.nodes.values();
	}

	public Collection<SkeletonLink> getLinks() {
		return this.links;
	}

}
