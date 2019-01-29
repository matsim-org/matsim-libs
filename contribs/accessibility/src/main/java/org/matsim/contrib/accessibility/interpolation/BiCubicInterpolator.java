package org.matsim.contrib.accessibility.interpolation;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.BivariateRealFunction;
import org.apache.commons.math.analysis.interpolation.BicubicSplineInterpolator;
import org.apache.commons.math.analysis.interpolation.BivariateRealGridInterpolator;
import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.gis.SpatialGrid;

/**
 * Interpolates data on a SpatialGrid with bicubic spline interpolation from apache (http://commons.apache.org).
 * 
 * Requires values on a SpatialGrid.
 * 
 * Problem: Wave effects may occur.
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
	 * Generates an interpolation function with BicubicSplineInterpolator from apache 
	 * (http://commons.apache.org/math/apidocs/org/apache/commons/math3/analysis/interpolation/BicubicSplineInterpolator.html).
	 * 
	 * @param sg the SpatialGrid to interpolate
	 */
	BiCubicInterpolator(SpatialGrid sg){
		this.sg= sg;
		sgNaNcheck();
		
		//create coordinate vectors for interpolation and a compatible array of values
		double[] x_coords= coord(sg.getXmin(), sg.getXmax(), sg.getResolution());
		double[] y_coords= coord(sg.getYmin(), sg.getYmax(), sg.getResolution());
		double[][] mirroredValues= sg.getMatrix();
		
		BivariateRealGridInterpolator interpolator = new BicubicSplineInterpolator();
		try {
			interpolatingFunction = interpolator.interpolate(y_coords, x_coords, mirroredValues);
		} catch (MathException e) {
			e.printStackTrace();
		} 
	}
	
	private void sgNaNcheck() {
		for (double y = this.sg.getYmin(); y <= this.sg.getYmax(); y += this.sg.getResolution()) {
			for (double x = this.sg.getXmin(); x <= this.sg.getXmax(); x += this.sg.getResolution()) {
				if (Double.isNaN(this.sg.getValue(x, y))){
					log.error("Bicubic spline interpolation doesn't work with NaN entries. " +
							"Please use bounding box data or shapefile data without NaN entries.");
					return;
				}
			}
		}
	}

	/**
	 * Interpolates the value at an arbitrary point with bicubic spline interpolation from apache.
	 * 
	 * @param xCoord the x-coordinate of the point to interpolate
	 * @param yCoord the y-coordinate of the point to interpolate
	 * @return interpolated value on the point (xCoord, yCoord)
	 */
	double biCubicInterpolation(double xCoord, double yCoord){
		try {
			return interpolatingFunction.value(yCoord, xCoord);
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		}
		return Double.NaN;
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
