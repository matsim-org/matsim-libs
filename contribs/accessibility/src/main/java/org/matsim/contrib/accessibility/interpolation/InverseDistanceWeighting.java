package org.matsim.contrib.accessibility.interpolation;

import org.matsim.contrib.accessibility.SpatialGrid;

/**
 * Implements inverse distance weighting for interpolation. Own implementation (no suitable implementation found).
 * 
 * Requires values on a SpatialGrid.
 * 
 * Problem: Peaks and valleys occur.
 * 
 * For more information see e.g.: 
 * http://www.geography.hunter.cuny.edu/~jochen/GTECH361/lectures/lecture11/concepts/Inverse%20Distance%20Weighted.htm
 * or: http://gisbsc.gis-ma.org/GISBScL7/de/html/VL7a_V_lo7.html (German).
 * 
 * @author tthunig
 * 
 */
class InverseDistanceWeighting {

	private SpatialGrid sg = null;
	
	/**
	 * Prepares the interpolation with the inverse distance weighting method.
	 * 
	 * @param sg the SpatialGrid to interpolate
	 */
	InverseDistanceWeighting(SpatialGrid sg){
		this.sg= sg;
	}
	
	/**
	 * Interpolates the value on a arbitrary point with inverse distance weighting.
	 * Considers only four neighboring values because this method needs less time for calculation than considering all known values 
	 * and the result is even more suitable for accessibility interpolation.
	 * 
	 * @param xCoord the x-coordinate of the point to interpolate
	 * @param yCoord the y-coordinate of the point to interpolate
	 * @param exponent the exponent for the inverse distance weighting
	 * @return interpolated value on the point (xCoord, yCoord)	 * 
	 */
	double inverseDistanceWeighting(double xCoord, double yCoord, double exponent){
		return fourNeighborsIDW(this.sg, xCoord, yCoord, exponent);
	}
	
	/**
	 * Interpolates a value at the given point (xCoord, yCoord) with the inverse distance weighting with variable power of weights. 
	 * Considers only four neighboring values.
	 * 
	 * @param sg the SpatialGrid with the known values
	 * @param xCoord
	 * @param yCoord
	 * @param exp the exponent for the weights. standard values are one or two.
	 * @return interpolated value at (xCoord, yCoord)
	 */
	static double fourNeighborsIDW(SpatialGrid sg, double xCoord, double yCoord, double exp) {
		double xDif= (xCoord-sg.getXmin()) % sg.getResolution();
		double yDif= (yCoord-sg.getYmin()) % sg.getResolution();
		
		//known value
		if (xDif==0 && yDif==0){
			return sg.getValue(xCoord, yCoord);
		}
		
		double x1= xCoord-xDif;
		double x2= x1+sg.getResolution();
		double y1= yCoord-yDif;
		double y2= y1+sg.getResolution();
		
		//calculate distances to the 4 neighboring sampling points
		double d11= Math.pow(distance(x1, y1, xCoord, yCoord), exp);
		double d12= Math.pow(distance(x1, y2, xCoord, yCoord), exp);
		double d21= Math.pow(distance(x2, y1, xCoord, yCoord), exp);
		double d22= Math.pow(distance(x2, y2, xCoord, yCoord), exp);
		
		//interpolation at the boundary
		if (xCoord == sg.getXmax()){
			//consider only 2 neighboring sampling points (up and down)
			return (sg.getValue(x1, y1)/d11 + sg.getValue(x1, y2)/d12) / (1/d11 + 1/d12);
		}
		if (yCoord == sg.getYmax()){
			//consider only 2 neighboring sampling points (left and right)
			return (sg.getValue(x1, y1)/d11 + sg.getValue(x2, y1)/d21) / (1/d11 + 1/d21);
		}
		
		//interpolation with 4 neighboring sampling points
		return (sg.getValue(x1, y1)/d11 + sg.getValue(x1, y2)/d12 + sg.getValue(x2, y1)/d21 + sg.getValue(x2, y2)/d22) 
				/ (1/d11 + 1/d12 + 1/d21 + 1/d22);
	}
	
	/**
	 * Calculates the distance between two given points in the plane.
	 * 
	 * @param x1 the x-coordinate of point 1
	 * @param y1 the y-coordinate of point 1
	 * @param x2 the x-coordinate of point 2
	 * @param y2 the y-coordinate of point 2
	 * @return distance between the points (x1,y1) and (x2,y2)
	 */
	private static double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((y2-y1)*(y2-y1) + (x2-x1)*(x2-x1));
	}
	
	/**
	 * Attention: Experimental version. Not tested sufficiently. Requires too much calculation time.
	 * 
	 * Interpolates a value at the given point (xCoord, yCoord) with the inverse distance weighting with variable power of weights:
	 * z(u_0)= Sum((1/d_i^exp)*z(u_i)) / Sum (1/d_i^exp).
	 * Needs more time for calculation than fourNeighborsIDW and the result is even less suitable for accessibility interpolation.
	 * 
	 * @param sg the SpatialGrid with the known values
	 * @param xCoord
	 * @param yCoord
	 * @param exp the exponent for the weights. standard values are one or two.
	 * @return interpolated value at (xCoord, yCoord)
	 */
	@Deprecated
	static double allValuesIDW(SpatialGrid sg, double xCoord, double yCoord, double exp) {
		double xDif= (xCoord-sg.getXmin()) % sg.getResolution();
		double yDif= (yCoord-sg.getYmin()) % sg.getResolution();
		
		//known value
		if (xDif==0 && yDif==0){
			return sg.getValue(xCoord, yCoord);
		}
		
		//interpolation with all known sampling points
		double distanceSum=0;
		double currentWeight=1;
		double weightSum=0;
		for (double y = sg.getYmin(); y <= sg.getYmax(); y += sg.getResolution()){
			for (double x = sg.getXmin(); x <= sg.getXmax(); x += sg.getResolution()){
				currentWeight= Math.pow(distance(x, y, xCoord, yCoord), exp);
				distanceSum+= sg.getValue(x, y)/currentWeight;
				weightSum+= 1/currentWeight;
			}
		}
		return distanceSum/weightSum;		
	}
	
}
