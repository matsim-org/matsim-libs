package playground.wdoering.oldstufffromgregor;


import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;

public class MyDataContainer {
	public static final String ELEMENT_NAME = "myDataContainer";

	private QuadTree<Coordinate> quad;
	private QuadTree<float[]> floatSegQuad;
	
	
	public void setFloatSegQuad(QuadTree<float[]> q) {
		this.floatSegQuad = q;
	}
	
	public QuadTree<float[]> getFloatSegQuad(){
		return this.floatSegQuad;
	}
	
	public void setDenseCoordsQuadTree(QuadTree<Coordinate> quad) {
		this.quad = quad;
	}

	public QuadTree<Coordinate> getDenseCoordsQuadTree() {
		return this.quad;
	}

}
