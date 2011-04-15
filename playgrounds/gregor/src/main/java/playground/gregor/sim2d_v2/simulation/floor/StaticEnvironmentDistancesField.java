package playground.gregor.sim2d_v2.simulation.floor;

import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;

public class StaticEnvironmentDistancesField {
	
	private QuadTree<EnvironmentDistances> quadTree;
	private double resoltution;
	private double maxSensingRange;

	public StaticEnvironmentDistancesField(QuadTree<EnvironmentDistances> quadTree,  double maxSensingRange, double  resolution) {
		this.quadTree = quadTree;
		this.resoltution = resolution;
		this.maxSensingRange = maxSensingRange;
	}
	
	

	/**
	 * The spatial resolution of the static environment distances field
	 * @return spatial resolution
	 */
	public double getStaticEnvironmentDistancesFieldResolution() {
		return this.resoltution;
	}
	
	/**
	 * The max sensing range for the environment. Objects outside the sensing range are ignored
	 * @return max sensing range
	 */
	public double getMaxSensingRange() {
		return this.maxSensingRange;
	}
	
	/**
	 *
	 * @param location
	 * @return distances next to location
	 */
	public EnvironmentDistances getEnvironmentDistances(Coordinate location) {
		return this.quadTree.get(location.x, location.y);
	}

	/**
	 * The quad tree that holds the environment distances
	 * @return quad tree
	 */
	public QuadTree<EnvironmentDistances> getEnvironmentDistancesQuadTree() {
		return this.quadTree;
	}
}
