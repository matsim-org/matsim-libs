package playground.gregor.sim2d.simulation;

import java.util.Collection;

import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;

public class StaticForceField {
	
	private final QuadTree<Force> forceQuad;

	protected StaticForceField(QuadTree<Force> forceQuad) {
		this.forceQuad = forceQuad;
	}

	public Force getForceWithin(Coordinate location, double range) {
		if (this.forceQuad.get(location.x, location.y,range).size() > 0) {
			return this.forceQuad.get(location.x, location.y);
		}
		return null;
	}

	public Collection<Force> getForces() {
		return this.forceQuad.values();
	}

	public void addForce(Force force) {
		this.forceQuad.put(force.getXCoord(),force.getYCoord(), force);		
	}
}
