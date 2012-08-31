package playground.andreas.aas.modules.cellBasedAccessibility.interpolation;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.BivariateRealFunction;
import org.apache.commons.math.analysis.interpolation.BicubicSplineInterpolator;
import org.apache.commons.math.analysis.interpolation.BivariateRealGridInterpolator;
import org.apache.log4j.Logger;

import playground.andreas.aas.modules.cellBasedAccessibility.gis.SpatialGrid;

/**
 * Interpolates data on a SpatialGrid with bicubic spline interpolation from apache (http://commons.apache.org).
 * 
 * Requires values on a SpatialGrid.
 * 
 * Problem: Wave effect occurs.
 * 
 * @author tthunig
 *
 */
class BiCubicInterpolator {

	private static final Logger log = Logger.getLogger(BiCubicInterpolator.class);
	
	private BivariateRealFunction interpolatingFunction = null;
	
	private SpatialGrid sg = null;
	
	/**
	 * Prepares bicubic spline interpolation:
	 * Generates interpolation function with BicubicSplineInterpolator from apache (http://commons.apache.org/math/apidocs/org/apache/commons/math3/analysis/interpolation/BicubicSplineInterpolator.html).
	 * 
	 * @param sg the SpatialGrid to interpolate
	 */
	BiCubicInterpolator(SpatialGrid sg){
		this.sg= sg;
		sgNaNcheck();
		
		//create default coordinates for interpolation and compatible array of values
		double[] x_default= coord(0, sg.getNumCols(0)-1, 1);
		double[] y_default= coord(0, sg.getNumRows()-1, 1);
		double[][] mirroredValues= sg.getMatrix();
		
		BivariateRealGridInterpolator interpolator = new BicubicSplineInterpolator();
		try {
			interpolatingFunction = interpolator.interpolate(y_default, x_default, mirroredValues); //needs default coordinates (0,1,2,...)
		} catch (MathException e) {
			e.printStackTrace();
		} 
	}
	
	private void sgNaNcheck() {
		for (double y = this.sg.getYmin(); y <= this.sg.getYmax(); y += this.sg.getResolution()) {
			for (double x = this.sg.getXmin(); x <= this.sg.getXmax(); x += this.sg.getResolution()) {
				if (Double.isNaN(this.sg.getValue(x, y))){
					log.error("Bicubic spline interpolation is not usefull for shapefile data, because it doesn't work with NaN entries. Please use bounding box data without NaN entries.");
					return;
				}
			}
		}
	}

	/**
	 * Interpolates the value on a arbitrary point with bicubic spline interpolation from apache.
	 * 
	 * @param xCoord the x-coordinate of the point to interpolate
	 * @param yCoord the y-coordinate of the point to interpolate
	 * @return interpolated value on the point (xCoord, yCoord)
	 */
	double biCubicInterpolation(double xCoord, double yCoord){
		try {
			return interpolatingFunction.value(transform(yCoord, this.sg.getYmin(), this.sg.getResolution()), transform(xCoord, this.sg.getXmin(), this.sg.getResolution()));
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		}
		return Double.NaN;
	}
	
	/**
	 * Transforms a given coordinate into their default value in the system of base coordinates (0,1,...).
	 * 
	 * @param coord 
	 * @param min the minimum value for this coordinate where a value is known at
	 * @param res the resolution of the SpatialGrid
	 * @return transformed coordinate between 0 and the number of known values in this coordinate direction
	 */
	private static double transform(double coord, double min, double res) {
		return (coord-min)/res;
	}

	/**
	 * Creates a coordinate vector.
	 * 
	 * @param min the minimum coordinate
	 * @param max the maximum coordinate
	 * @param resolution
	 * @return coordinate vector from min to max with the given resolution
	 */
	private static double[] coord(double min, double max, double resolution) {
		double[] coord = new double[(int) ((max - min) / resolution) + 1];
		coord[0] = min;
		for (int i = 1; i < coord.length; i++) {
			coord[i] = min + i * resolution;
		}
		return coord;
	}
	
}
