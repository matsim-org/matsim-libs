package interpolation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

public class Interpolation {

	private static final Logger log = Logger.getLogger(Interpolation.class);
	
	public static final int BILINEAR = 0;
	public static final int BICUBIC = 1;
	public static final int INVERSE_DISTANCE_WEIGHTING = 2;
	
	private SpatialGrid sg = null;
	private BiCubicInterpolator biCubicInterpolator = null;
	
	private double exp = 1.;
	private int interpolationMethod = -1;
	
	/**
	 * constructor
	 * 
	 * @param sg the SpatialGrid to interpolate
	 * @param method the interpolation method
	 */
	public Interpolation(SpatialGrid sg, final int method ){
	
		this(sg, method, 1);
	}
	
	/**
	 * constructor
	 * 
	 * @param sg the SpatialGrid to interpolate
	 * @param method the interpolation method
	 * @param exp the exponent for weights, only necessary in the inverse distance weighting method
	 */
	public Interpolation(SpatialGrid sg, final int method, final double exp ){
		
		this.sg = sg;
		this.interpolationMethod = method;
		this.exp = exp;
		if(this.interpolationMethod == BICUBIC){
			log.info("Creating object for bicubic interpolation ...");
			this.biCubicInterpolator= new BiCubicInterpolator(this.sg);
		}
		if(this.interpolationMethod == INVERSE_DISTANCE_WEIGHTING){
			log.warn("IDW interpolation not usefull for accessibility interpolation.");
		}
	}

	/**
	 * calculates the value at the given coordinate
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
		case 0: return MyBiLinearInterpolator.myBiLinearValueInterpolation(this.sg, x, y);
		case 1: return biCubicInterpolator.biCubicInterpolation(x, y);
		case 2: return MyInverseDistanceWeighting.my4NeighborsIDW(this.sg, x, y, exp);		
		}
		return Double.NaN;
	}
	
	/**
	 * for testing
	 * @param args
	 */
	public static void main(String args[]){
		
//		Interpolation i = new Interpolation(Interpolation.BICUBIC);
		
	}
	
	
}
