package interpolation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

/**
 * wrapper class for interpolation
 * interpolates the value at one point from grid data as SpatialGrid
 * offers bilinear interpolation, bicubic spline interpolation and inverse distance weighting
 * 
 * bilinear interpolation:
 * 	uses linear spline interpolation with separation (first horizontal then vertical). own implementation.
 * 
 * bicubic spline interpolation:
 * 	uses BicubicSplineInterpolator from apache (http://commons.apache.org/math/apidocs/org/apache/commons/math3/analysis/interpolation/BicubicSplineInterpolator.html).
 * 	wave effect occurs.
 * 
 * inverse distance weighting:
 * 	please do not use (experimental version)!
 *  uses inverse distance weighting for the 4 nearest known values. own implementation.
 *  not useful for accessibility interpolation because peaks and valleys occur.
 *  for more information see e.g.: http://www.geography.hunter.cuny.edu/~jochen/GTECH361/lectures/lecture11/concepts/Inverse%20Distance%20Weighted.htm
 *  or: http://gisbsc.gis-ma.org/GISBScL7/de/html/VL7a_V_lo7.html (in german)
 * 
 * @author tthunig
 *
 */
public class Interpolation {

	private static final Logger log = Logger.getLogger(Interpolation.class);
	
	public static final int BILINEAR = 0;
	public static final int BICUBIC = 1;
	/** please do not use (experimental version) **/
	public static final int INVERSE_DISTANCE_WEIGHTING_EXPERIMENTAL = 2;
	
	private SpatialGrid sg = null;
	private BiCubicInterpolator biCubicInterpolator = null;
	private BiLinearInterpolator biLinearInterpolator = null;
	
	/** only necessary for the inverse distance weighting method **/
	private double exp = 1.;
	private int interpolationMethod = -1;
	
	/**
	 * prepares interpolation with the selected interpolation method
	 * 
	 * if inverse distance weighting is chosen, the exponent for weights will be the default value 1
	 * 
	 * @param sg the SpatialGrid to interpolate
	 * @param method the interpolation method
	 */
	public Interpolation(SpatialGrid sg, final int method ){
	
		this(sg, method, 1);
	}
	
	/**
	 * prepares interpolation with the selected interpolation method
	 * 
	 * @param sg the SpatialGrid to interpolate
	 * @param method the interpolation method
	 * @param exp the exponent for weights. only considered if interpolation method is inverse distance weighting
	 */
	public Interpolation(SpatialGrid sg, final int method, final double exp ){
		
		this.sg = sg;
		this.interpolationMethod = method;
		this.exp = exp;
		if(this.interpolationMethod == BILINEAR){
			log.info("Preparing bilinear interpolation ...");
			this.biLinearInterpolator = new BiLinearInterpolator(this.sg);
		}
		if(this.interpolationMethod == BICUBIC){
			log.info("Preparing bicubic interpolation ...");
			this.biCubicInterpolator = new BiCubicInterpolator(this.sg);
		}
		if(this.interpolationMethod == INVERSE_DISTANCE_WEIGHTING_EXPERIMENTAL){
			log.warn("Please do not use IDW (experimental version)!");
			log.warn("IDW interpolation not useful for accessibility interpolation.");
		}
	}

	/**
	 * interpolates the value at the given coordinate
	 * 
	 * @param coord
	 * @return the interpolated value
	 */
	public double interpolate(Coord coord){
		if(sg != null && coord != null)
			return interpolate(coord.getX(), coord.getY());
		log.warn("Either the spatial grid is not initialized or the coordinates are zero!");
		return Double.NaN;
	}
	
	/**
	 * interpolates the value at the given coordinate (x,y)
	 * 
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @return the interpolated value at (x,y)
	 */
	public double interpolate(double x, double y){
		
		switch(this.interpolationMethod){
		case 0: return biLinearInterpolator.biLinearInterpolation(x, y);
		case 1: return biCubicInterpolator.biCubicInterpolation(x, y);
		case 2: return InverseDistanceWeighting.fourNeighborsIDW(this.sg, x, y, exp);		
		}
		return Double.NaN;
	}	
	
}
