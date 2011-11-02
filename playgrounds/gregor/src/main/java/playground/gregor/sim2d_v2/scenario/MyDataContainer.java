package playground.gregor.sim2d_v2.scenario;


import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;

public class MyDataContainer {

	private QuadTree<Coordinate> quad;

	public void setQuadTree(QuadTree<Coordinate> quad) {
		this.quad = quad;
	}

	public QuadTree<Coordinate> getQuadTree() {
		return this.quad;
	}

}
