package interpolation;

import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.BivariateRealFunction;
import org.apache.commons.math.analysis.interpolation.BicubicSplineInterpolator;
import org.apache.commons.math.analysis.interpolation.BivariateRealGridInterpolator;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

/**
 * interpolates data on a SpatialGrid with bicubic spline interpolation from apache (http://commons.apache.org)
 * 
 * @author tthunig
 *
 */
public class BiCubicInterpolator {

	static SpatialGrid biCubicGridInterpolation(SpatialGrid sg){
		// generate new coordinates for higher resolution
		double[] x_new = InterpolateSpatialGrid.coord(sg.getXmin(), sg.getXmax(), sg.getResolution() / 2);
		double[] y_new = InterpolateSpatialGrid.coord(sg.getYmin(), sg.getYmax(), sg.getResolution() / 2);
				
		// calculate new values for higher resolution
		SpatialGrid sg_new= new SpatialGrid(sg.getXmin(), sg.getYmin(),
				sg.getXmax(), sg.getYmax(), sg.getResolution() / 2);
		for (int i=0; i<y_new.length; i++){
			for (int j=0; j<x_new.length; j++){
				sg_new.setValue(sg_new.getRow(y_new[i]), sg_new.getColumn(x_new[j]), biCubicInterpolation(sg, x_new[j], y_new[i]));
			}
		}
		return sg_new;
	}
	
	/**
	 * interpolates the value on a arbitrary point with bicubic spline interpolation from apache
	 * requires values on a grid as SpatialGrid
	 * 
	 * @param sg the values on the grid as SpatialGrid
	 * @param xCoord the x-coordinate of the point to interpolate
	 * @param yCoord the y-coordinate of the point to interpolate
	 * @return interpolated value on the point (xCoord, yCoord)
	 */
	public static double biCubicInterpolation(SpatialGrid sg, double xCoord, double yCoord){
		double[] x_default= coord(0,sg.getMatrix()[0].length-1,1);
		double[] y_default= coord(0,sg.getMatrix().length-1,1);
		
		double[][] grid= flip(sg.getMatrix()); //TODO: ohne das ist es gespiegelt
		
		try {
			BivariateRealGridInterpolator interpolator = new BicubicSplineInterpolator();
			BivariateRealFunction func = interpolator.interpolate(y_default, x_default, sg.getMatrix()); //benoetigt default Koordinaten (0,1,2,...)
			
			return func.value(transform(yCoord, sg.getYmin(), sg.getResolution()), transform(xCoord, sg.getXmin(), sg.getResolution()));
		} catch (MathException e) {
			e.printStackTrace();
		} 
		
		return Double.NaN;
	}
	
	/**
	 * transforms a given coordinate into their default value in the system of base coordinates (0,1,...)
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
	 * creates a coordinate vector
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
	
	/**
	 * flips the given matrix horizontal
	 * 
	 * @param matrix
	 * @return the horizontal mirrored matrix
	 */
	private static double[][] flip(double[][] matrix) {
		double[][] flip= new double[matrix.length][matrix[0].length];
		for (int i=0; i<flip.length; i++){
			for (int j=0; j<flip[0].length; j++){
				flip[i][j]= matrix[matrix.length-1-i][j];
			}
		}
		return flip;
	}
	
}
