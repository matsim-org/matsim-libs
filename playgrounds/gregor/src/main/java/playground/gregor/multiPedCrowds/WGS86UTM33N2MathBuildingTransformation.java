package playground.gregor.multiPedCrowds;



import Jama.Matrix;

import com.vividsolutions.jts.geom.Coordinate;

public class WGS86UTM33N2MathBuildingTransformation {
	
	
	private final static double R = 0.000973309;
	private final static double Phi = -1.56728;
	private final static double x_0 = 386422.594;
	private final static double y_0 = 5819504.714;
	
	private static final Matrix m = new Matrix(new double[][]{new double[]{Math.cos(Phi),Math.sin(Phi)},new double[]{-Math.sin(Phi),Math.cos(Phi)}});
	
	public static Coordinate transform(Coordinate coord) {
		
	
		Matrix c = new Matrix(2,1);
		c.set(0,0, coord.x - x_0);
		c.set(1,0, coord.y - y_0);
		
		Matrix res = m.times(c);
		
		double x = 1/R *res.get(0, 0);
		double y = 1/R *res.get(1,0);
		double z = 0.;
		return new Coordinate(x,y,z);
		
		}

}
