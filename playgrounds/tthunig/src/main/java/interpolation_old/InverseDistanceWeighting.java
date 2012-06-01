package interpolation_old;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

/**
 * implements inverse distance weighting for interpolation. own implementation (no suitable implementation found).
 * requires values on a SpatialGrid
 * 
 * please not use (experimental version)!
 * not useful for accessibility interpolation because peaks and valleys occur.
 * for more information see e.g.: http://www.geography.hunter.cuny.edu/~jochen/GTECH361/lectures/lecture11/concepts/Inverse%20Distance%20Weighted.htm
 * or: http://gisbsc.gis-ma.org/GISBScL7/de/html/VL7a_V_lo7.html (german)
 * 
 * @author tthunig
 *
 */
@Deprecated
class InverseDistanceWeighting {

	/**
	 * interpolates a value at the given point (xCoord, yCoord) with the standard inverse distance weighting
	 * z(u_0)= Sum((1/d_i)*z(u_i)) / Sum (1/d_i)
	 * uses myAllValuesIDWFactor(SpatialGrid sg, double xCoord, double yCoord, double factor) with factor 1
	 * 
	 * @param sg the SpatialGrid with the known values
	 * @param xCoord
	 * @param yCoord
	 * @return interpolated value at (xCoord, yCoord)
	 */
	static double allValuesIDW(SpatialGrid sg, double xCoord, double yCoord){
		return allValuesIDW(sg, xCoord, yCoord, 1);
	}
	
	/**
	 * interpolates a value at the given point (xCoord, yCoord) with the inverse distance weighting with squared weights
	 * z(u_0)= Sum((1/d_i^2)*z(u_i)) / Sum (1/d_i^2)
	 * uses myAllValuesIDWFactor(SpatialGrid sg, double xCoord, double yCoord, double factor) with factor 2
	 * 
	 * @param sg the SpatialGrid with the known values
	 * @param xCoord
	 * @param yCoord
	 * @return interpolated value at (xCoord, yCoord)
	 */
	static double allValuesIDWSquare(SpatialGrid sg, double xCoord, double yCoord){
		return allValuesIDW(sg, xCoord, yCoord, 2);
	}
	
	/**
	 * interpolates a value at the given point (xCoord, yCoord) with the inverse distance weighting with variable power of weights
	 * z(u_0)= Sum((1/d_i^exp)*z(u_i)) / Sum (1/d_i^exp)
	 * 
	 * @param sg the SpatialGrid with the known values
	 * @param xCoord
	 * @param yCoord
	 * @param exp the exponent for the weights
	 * @return interpolated value at (xCoord, yCoord)
	 */
	static double allValuesIDW(SpatialGrid sg, double xCoord, double yCoord, double exp) {
		double xDif= (xCoord-sg.getXmin()) % sg.getResolution();
		double yDif= (yCoord-sg.getYmin()) % sg.getResolution();
		if (xDif==0){
			if (yDif==0){
				//known value
				return sg.getMatrix()[sg.getRow(yCoord)][sg.getColumn(xCoord)];
			}
		}
		
		//z(u_0)= Sum((1/d_i^exp)*z(u_i)) / Sum (1/d_i^exp)
		double[][] weights= new double[sg.getMatrix().length][sg.getMatrix()[0].length];
		double value=0;
		double weightsum=0;
		for (int i=0; i<weights.length; i++){
			for (int j=0; j<weights[0].length; j++){
				weights[i][j]= Math.pow(distance(sg.getXmin()+j*sg.getResolution(), sg.getYmin()+i*sg.getResolution(), xCoord, yCoord), exp);
				value+= sg.getMatrix()[i][j]/weights[i][j];
				weightsum+= 1/weights[i][j];
			}
		}
		return value/weightsum;		
	}
	
	/**
	 * interpolates a value at the given point (xCoord, yCoord) with the standard inverse distance weighting
	 * considers only 4 neighboring values
	 * uses my4NeighborsIDWFactor(SpatialGrid sg, double xCoord, double yCoord, double factor) with factor 1
	 * 
	 * @param sg the SpatialGrid with the known values
	 * @param xCoord
	 * @param yCoord
	 * @return interpolated value at (xCoord, yCoord)
	 */
	static double fourNeighborsIDW(SpatialGrid sg, double xCoord, double yCoord){
		return fourNeighborsIDW(sg, xCoord, yCoord, 1);
	}
	
	/**
	 * interpolates a value at the given point (xCoord, yCoord) with the inverse distance weighting with squared weights
	 * considers only 4 neighboring values
	 * uses my4NeighborsIDWFactor(SpatialGrid sg, double xCoord, double yCoord, double factor) with factor 2
	 * 
	 * @param sg the SpatialGrid with the known values
	 * @param xCoord
	 * @param yCoord
	 * @return interpolated value at (xCoord, yCoord)
	 */
	static double fourNeighborsIDWSquare(SpatialGrid sg, double xCoord, double yCoord){
		return fourNeighborsIDW(sg, xCoord, yCoord, 2);
	}
	
	/**
	 * interpolates a value at the given point (xCoord, yCoord) with the inverse distance weighting with variable power of weights 
	 * considers only 4 neighboring values
	 * 
	 * @param sg the SpatialGrid with the known values
	 * @param xCoord
	 * @param yCoord
	 * @param exp the exponent for the weights
	 * @return interpolated value at (xCoord, yCoord)
	 */
	static double fourNeighborsIDW(SpatialGrid sg, double xCoord, double yCoord, double exp) {
		double xDif= (xCoord-sg.getXmin()) % sg.getResolution();
		double yDif= (yCoord-sg.getYmin()) % sg.getResolution();
		if (xDif==0){
			if (yDif==0){
				//known value
				return sg.getMatrix()[sg.getRow(yCoord)][sg.getColumn(xCoord)];
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
			return (sg.getMatrix()[sg.getRow(y1)][sg.getColumn(x1)]/d11
					+ sg.getMatrix()[sg.getRow(y2)][sg.getColumn(x1)]/d12) 
					/ (1/d11 + 1/d12);
		}
		if (yCoord == sg.getYmax()){
			//consider only 2 neighbors (up and down)
			return (sg.getMatrix()[sg.getRow(y1)][sg.getColumn(x1)]/d11
					+ sg.getMatrix()[sg.getRow(y1)][sg.getColumn(x2)]/d21) 
					/ 1/11 + 1/d21;
		}
		
		double value= (
				+ sg.getMatrix()[sg.getRow(y2)][sg.getColumn(x1)]/d12 
				+ sg.getMatrix()[sg.getRow(y1)][sg.getColumn(x2)]/d21 
				+ sg.getMatrix()[sg.getRow(y2)][sg.getColumn(x2)]/d22) 
				/ (1/d11 + 1/d12 + 1/d21 + 1/d22);
		return value;
	}
	
	/**
	 * calculates the distance between two given points in the plane
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
	
}
