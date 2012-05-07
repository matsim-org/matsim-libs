package interpolation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

public class Interpolation {

	private static final Logger log = Logger.getLogger(Interpolation.class);
	
	public static final int BILINEAR = 0;
	public static final int BICUBIC = 1;
	public static final int INVERSE_DISTANCE_WEIGHTING = 2;
	
	private double exp = 1.;
	private int interpolationMethod = -1;
	
	/**
	 * constructor
	 * @param method
	 */
	public Interpolation(final int method ){
		
		this.interpolationMethod = method;
		this.exp = 1.;
		
	}
	
	/**
	 * constructor
	 * @param method
	 * @param exp
	 */
	public Interpolation(final int method, final double exp ){
		
		this.interpolationMethod = method;
		this.exp = exp;
		
	}

	public double interpolate(SpatialGrid sg, Coord coord){
		if(sg != null && coord != null)
			return interpolate(sg, coord.getX(), coord.getY());
		log.warn("ERROR");
		return Double.NaN;
	}
	
	public double interpolate(SpatialGrid sg, double x, double y){
		
		switch(this.interpolationMethod){
		case 0: return MyBiLinearInterpolator.myBiLinearValueInterpolation(sg, x, y);
		case 1: // TODO
				log.warn("BiCubic interpolation not available yet!");
				break;
		case 2: log.warn("IDW interpolation not tested yet!");
				return MyInverseDistanceWeighting.my4NeighborsIDW(sg, x, y, exp);		
		}
		return Double.NaN;
	}
	
	/**
	 * for testing
	 * @param args
	 */
	public static void main(String args[]){
		
		Interpolation i = new Interpolation(0);
		
	}
	
	
}
