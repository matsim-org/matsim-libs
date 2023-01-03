package org.matsim.contrib.accessibility.interpolation;

import org.matsim.contrib.accessibility.SpatialGrid;

/**
 * Implements bilinear interpolation.
 * Uses linear spline interpolation with separation: first horizontal then vertical.
 * Own implementation (no suitable implementation found).
 * 
 * Requires values on a SpatialGrid.
 * 
 * @author tthunig
 * 
 */
class BiLinearInterpolator {

	private SpatialGrid sg = null;
	
	/**
	 * Prepares bilinear interpolation.
	 * 
	 * @param sg the SpatialGrid to interpolate
	 */
	BiLinearInterpolator(SpatialGrid sg){
		this.sg= sg;
	}
	
	/**
	 * Interpolates the value on a arbitrary point with bilinear interpolation.
	 * 
	 * @param xCoord the x-coordinate of the point to interpolate
	 * @param yCoord the y-coordinate of the point to interpolate
	 * @return interpolated value on the point (xCoord, yCoord)
	 */
	double biLinearInterpolation(double xCoord, double yCoord){
		return biLinearValueInterpolation(this.sg, xCoord, yCoord);
	}
	
	/**
	 * Interpolates the value on a arbitrary point with bilinear interpolation.
	 * Requires values on a grid as SpatialGrid.
	 * 
	 * @param sg the values on the grid as SpatialGrid
	 * @param xCoord the x-coordinate of the point to interpolate
	 * @param yCoord the y-coordinate of the point to interpolate
	 * @return interpolated value on the point (xCoord, yCoord)
	 */
	static double biLinearValueInterpolation(SpatialGrid sg, double xCoord, double yCoord){
		double xDif= (xCoord-sg.getXmin()) % sg.getResolution();
		double yDif= (yCoord-sg.getYmin()) % sg.getResolution();
		
		//corner coordinates of the grid cell
		double x1= xCoord-xDif;
		double x2= x1+sg.getResolution();
		double y1= yCoord-yDif;
		double y2= y1+sg.getResolution();
		
		double xWeight= xDif/sg.getResolution();
		double yWeight= yDif/sg.getResolution();
		
		//case differentiation important for boundary data of shapefiles, because of neighboring NaN values
		if (xDif==0){
			if (yDif==0){
				//known value
				return sg.getValue(xCoord, yCoord);
			}
			//point to interpolate lies at the grid cell boundary
			return sg.getValue(x1, y1)*(1-yWeight) + sg.getValue(x1, y2)*yWeight;
		}
		if (yDif==0){
			//point to interpolate lies at the grid cell boundary
			return sg.getValue(x1, y1)*(1-xWeight) + sg.getValue(x2, y1)*xWeight;
		}
		
		//interpolates first in y-direction then in x-direction with linear splines
		return (sg.getValue(x1, y1)*(1-yWeight) + sg.getValue(x1, y2)*yWeight) * (1-xWeight) 
				+ (sg.getValue(x2, y1)*(1-yWeight) + sg.getValue(x2, y2)*yWeight) * xWeight;
	}
}
