package org.matsim.contrib.accessibility.interpolation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.accessibility.SpatialGrid;

/**
 * Wrapper class for interpolation.
 * Interpolates the value at one point from grid data as SpatialGrid.
 * Offers bilinear interpolation, bicubic spline interpolation and inverse distance weighting.
 * 
 * Bilinear interpolation:
 * 	Uses linear spline interpolation with separation (first horizontal then vertical). Own implementation.
 * 
 * Bicubic spline interpolation:
 * 	Uses BicubicSplineInterpolator from apache (http://commons.apache.org/math/apidocs/org/apache/commons/math3/analysis/interpolation/BicubicSplineInterpolator.html).
 * 	Wave effects may occur.
 * 
 * Inverse distance weighting:
 *  Uses inverse distance weighting for the 4 nearest known values. Own implementation.
 *  Peaks and valleys occur.
 *  For more information see e.g.: http://www.geography.hunter.cuny.edu/~jochen/GTECH361/lectures/lecture11/concepts/Inverse%20Distance%20Weighted.htm
 *  or: http://gisbsc.gis-ma.org/GISBScL7/de/html/VL7a_V_lo7.html (in German).
 * 
 * @author tthunig
 *
 */
class Interpolation {

	private static final Logger log = Logger.getLogger(Interpolation.class);
	
	public static final int BILINEAR = 0;
	public static final int BICUBIC = 1;
	public static final int INVERSE_DISTANCE_WEIGHTING = 2;
	
	private SpatialGrid sg = null;
	private BiCubicInterpolator biCubicInterpolator = null;
	private BiLinearInterpolator biLinearInterpolator = null;
	private InverseDistanceWeighting inverseDistanceWeighting = null;
	
	/** exponent only necessary for the inverse distance weighting method **/
	private double exp = 1.;
	private int interpolationMethod = -1;
	
	/**
	 * Prepares interpolation with the selected interpolation method.
	 * 
	 * If inverse distance weighting is chosen, the exponent for weights will be the default value 1.
	 * 
	 * @param sg the SpatialGrid to interpolate
	 * @param method the interpolation method
	 */
	public Interpolation(SpatialGrid sg, final int method ){
		
		this(sg, method, 1);
	}
	
	/**
	 * Prepares interpolation with the selected interpolation method.
	 * 
	 * @param sg the SpatialGrid to interpolate
	 * @param method the interpolation method
	 * @param exp the exponent for weights. only considered if interpolation method is inverse distance weighting. standard values are one or two.
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
		if(this.interpolationMethod == INVERSE_DISTANCE_WEIGHTING){
			log.info("Preparing interpolation with the inverse distance weighting method ...");
			this.inverseDistanceWeighting = new InverseDistanceWeighting(this.sg);
		}
	}

	/**
	 * Interpolates the value at the given coordinate.
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
	 * Interpolates the value at the given coordinate (x,y).
	 * 
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @return the interpolated value at (x,y)
	 */
	public double interpolate(double x, double y){
		
		switch(this.interpolationMethod){
		case 0: return this.biLinearInterpolator.biLinearInterpolation(x, y);
		case 1: return this.biCubicInterpolator.biCubicInterpolation(x, y);
		case 2: return this.inverseDistanceWeighting.inverseDistanceWeighting(x, y, this.exp);		
		}
		return Double.NaN;
	}

	public int getInterpolationMethod() {
		return interpolationMethod;
	}	
	
}
