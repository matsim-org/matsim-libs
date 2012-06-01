package interpolation;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

/**
 * implements bilinear interpolation
 * uses linear spline interpolation with separation: first horizontal then vertical
 * own implementation (no suitable implementation found)
 * 
 * requires values on a grid, either as double[][] or as SpatialGrid
 * 
 * @author tthunig
 *
 */
class BiLinearInterpolator {

	private SpatialGrid sg = null;
	
	/**
	 * prepares bilinear interpolation
	 * 
	 * @param sg the SpatialGrid to interpolate
	 */
	BiLinearInterpolator(SpatialGrid sg){
		this.sg= sg;
	}
	
	/**
	 * interpolates the value on a arbitrary point with bilinear interpolation
	 * 
	 * @param xCoord the x-coordinate of the point to interpolate
	 * @param yCoord the y-coordinate of the point to interpolate
	 * @return interpolated value on the point (xCoord, yCoord)
	 */
	double biLinearInterpolation(double xCoord, double yCoord){
		return biLinearValueInterpolation(this.sg, xCoord, yCoord);
	}
	
	/**
	 * interpolates the value on a arbitrary point with bilinear interpolation
	 * requires values on a grid as SpatialGrid
	 * 
	 * @param sg the values on the grid as SpatialGrid
	 * @param xCoord the x-coordinate of the point to interpolate
	 * @param yCoord the y-coordinate of the point to interpolate
	 * @return interpolated value on the point (xCoord, yCoord)
	 */
	static double biLinearValueInterpolation(SpatialGrid sg, double xCoord, double yCoord){
		double xDif= (xCoord-sg.getXmin()) % sg.getResolution();
		double yDif= (yCoord-sg.getYmin()) % sg.getResolution();
		
		double x1= xCoord-xDif;
		double x2= x1+sg.getResolution();
		double y1= yCoord-yDif;
		double y2= y1+sg.getResolution();
		
		double xWeight= xDif/sg.getResolution();
		double yWeight= yDif/sg.getResolution();
		
		if (xDif==0){
			if (yDif==0){
				//xWeigt=yWeight=0
				return sg.getMatrix()[sg.getRow(yCoord)][sg.getColumn(xCoord)];
			}
			//xWeight=0
			return sg.getMatrix()[sg.getRow(y1)][sg.getColumn(x1)]*(1-yWeight) + sg.getMatrix()[sg.getRow(y2)][sg.getColumn(x1)]*yWeight;
		}
		if (yDif==0){
			//yWeight=0
			return sg.getMatrix()[sg.getRow(y1)][sg.getColumn(x1)]*(1-xWeight) + sg.getMatrix()[sg.getRow(y1)][sg.getColumn(x2)]*xWeight;
		}
		
		return (sg.getMatrix()[sg.getRow(y1)][sg.getColumn(x1)]*(1-yWeight) + sg.getMatrix()[sg.getRow(y2)][sg.getColumn(x1)]*yWeight) * (1-xWeight) + (sg.getMatrix()[sg.getRow(y1)][sg.getColumn(x2)]*(1-yWeight) + sg.getMatrix()[sg.getRow(y2)][sg.getColumn(x2)]*yWeight) * xWeight;
	}
}
