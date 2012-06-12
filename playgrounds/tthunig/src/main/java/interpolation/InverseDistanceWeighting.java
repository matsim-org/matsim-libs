package interpolation;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

/**
 * Implements inverse distance weighting for interpolation. Own implementation (no suitable implementation found).
 * 
 * Requires values on a SpatialGrid.
 * 
 * Problem: Peaks and valleys occur.
 * 
 * For more information see e.g.: http://www.geography.hunter.cuny.edu/~jochen/GTECH361/lectures/lecture11/concepts/Inverse%20Distance%20Weighted.htm
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
	 * Considers only four neighboring values because this method needs less time for calculation than considering all known values and the result is not much different.
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
		if (xDif==0){
			if (yDif==0){
				//known value
				return sg.getValue(xCoord, yCoord);
			}
		}
		
		double x1= xCoord-xDif;
		double x2= x1+sg.getResolution();
		double y1= yCoord-yDif;
		double y2= y1+sg.getResolution();
		
		double d11= Math.pow(distance(x1, y1, xCoord, yCoord), exp);
		double d12= Math.pow(distance(x1, y2, xCoord, yCoord), exp);
		double d21= Math.pow(distance(x2, y1, xCoord, yCoord), exp);
		double d22= Math.pow(distance(x2, y2, xCoord, yCoord), exp);
		
		//interpolation on the boundary
		if (xCoord == sg.getXmax()){
			//consider only 2 neighbors (left and right)
			return (sg.getValue(x1, y1)/d11 + sg.getValue(x1, y2)/d12) / (1/d11 + 1/d12);
		}
		if (yCoord == sg.getYmax()){
			//consider only 2 neighbors (up and down)
			return (sg.getValue(x1, y1)/d11 + sg.getValue(x2, y1)/d21) / 1/11 + 1/d21;
		}
		
		double value= (sg.getValue(x1, y1)/d11 + sg.getValue(x1, y2)/d12 + sg.getValue(x2, y1)/d21 + sg.getValue(x2, y2)/d22) 
				/ (1/d11 + 1/d12 + 1/d21 + 1/d22);
		return value;
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
	 * Interpolates a value at the given point (xCoord, yCoord) with the inverse distance weighting with variable power of weights:
	 * z(u_0)= Sum((1/d_i^exp)*z(u_i)) / Sum (1/d_i^exp).
	 * Needs more time for calculation than fourNeighborsIDW and the result is not much different.
	 * 
	 * @param sg the SpatialGrid with the known values
	 * @param xCoord
	 * @param yCoord
	 * @param exp the exponent for the weights. standard values are one or two.
	 * @return interpolated value at (xCoord, yCoord)
	 */
	static double allValuesIDW(SpatialGrid sg, double xCoord, double yCoord, double exp) {
		double xDif= (xCoord-sg.getXmin()) % sg.getResolution();
		double yDif= (yCoord-sg.getYmin()) % sg.getResolution();
		if (xDif==0){
			if (yDif==0){
				//known value
				return sg.getValue(xCoord, yCoord);
			}
		}
		
		//z(u_0)= Sum((1/d_i^exp)*z(u_i)) / Sum (1/d_i^exp)
		double value=0;
		double currentWeight=1;
		double weightsum=0;
		for (int row=0; row<sg.getNumRows(); row++){
			for (int col=0; col<sg.getNumCols(0); col++){
				currentWeight= Math.pow(distance(sg.getXmin()+col*sg.getResolution(), sg.getYmax()-row*sg.getResolution(), xCoord, yCoord), exp);
				value+= sg.getValue(xCoord, yCoord)/currentWeight;
				weightsum+= 1/currentWeight;
			}
		}
		return value/weightsum;		
	}
	
}
