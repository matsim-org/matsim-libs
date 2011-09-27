package playground.gregor.sim2d_v2.experimental;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class SkeletonNode {

	private final Point location;

	private final Set<SkeletonLink> linkedLinks = new HashSet<SkeletonLink>();

	private final Id id;



	public SkeletonNode(Point from, Id id) {
		this.location = from;
		this.id = id;
	}

	public Coordinate getCoord() {
		return this.location.getCoordinate();
	}

	public Geometry getGeometry() {
		return this.location;
	}


	public Set<SkeletonLink> getLinkedLinks() {
		return this.linkedLinks;
	}

	public void linkLink(SkeletonLink l) {
		this.linkedLinks.add(l);
	}

	public boolean isIntersectionOrDeadEnd() {

		return (this.linkedLinks.size() > 2 || this.linkedLinks.size() == 1);

	}

	public boolean removeLink(SkeletonLink link) {
		return this.linkedLinks.remove(link);
	}

	public Id getId() {
		return this.id;
	}
}
