package interpolation_old;

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
	 * old implementation without SpatialGrid
	 * please use myBiLinearValueInterpolation(SpatialGrid sg, double xCoord, double yCoord)
	 * 
	 * interpolates the values on a grid given as double[][] with bilinear interpolation to a higher resolution
	 * 
	 * @param values the known values on the grid
	 * @return grid with higher resolution
	 */
	@Deprecated
	static double[][] biLinearGridInterpolation(double[][] values){
		double[][] interp_values= new double[Interpolate.coordLength(1, values.length)][Interpolate.coordLength(1, values[0].length)];
		
		// calculate new values for higher resolution
		for (int i=0; i<interp_values.length; i++){
			for (int j=0; j<interp_values[0].length; j++){
				//i and j even - old grid value
				if (i%2==0 && j%2==0){
					interp_values[i][j]= values[i/2][j/2];
				}
				//i and j odd - old grid center -> calculate mean value of the 4 neighboring grid corners
				else if (i%2==1 && j%2==1){
					interp_values[i][j]= (values[i/2][j/2] + values[i/2][(j/2)+1] + values[(i/2)+1][j/2] + values[(i/2)+1][(j/2)+1])/4;
				}
				//either i or j even - value on old grid edge -> calculate mean value of the 2 neighboring grid corners
				else if (i%2==0){
					interp_values[i][j]= (values[i/2][j/2] + values[i/2][(j/2)+1])/2;
				}
				else{ //j%2==0
					interp_values[i][j]= (values[i/2][j/2] + values[(i/2)+1][j/2])/2;
				}
			}
		}
		return interp_values;
	}
	
	/**
	 * just for testing
	 * please use myBiLinearValueInterpolation(SpatialGrid sg, double xCoord, double yCoord)
	 * 
	 * interpolates the values on a grid given as SpatialGrid with bilinear interpolation to a higher resolution
	 * 
	 * @param sg the SpatialGrid to interpolate
	 * @return SpatialGrid with higher resolution
	 */
	@Deprecated
	static SpatialGrid biLinearGridInterpolation(SpatialGrid sg){
		// generate new coordinates for higher resolution
		double[] x_new = coord(sg.getXmin(), sg.getXmax(), sg.getResolution() / 2);
		double[] y_new = coord(sg.getYmin(), sg.getYmax(), sg.getResolution() / 2);
		
		// calculate new values for higher resolution
		SpatialGrid sg_new= new SpatialGrid(sg.getXmin(), sg.getYmin(),
				sg.getXmax(), sg.getYmax(), sg.getResolution() / 2);
		for (int i=0; i<y_new.length; i++){
			for (int j=0; j<x_new.length; j++){
				//i and j even - old grid value
				if (i%2==0 && j%2==0){
					sg_new.setValue(sg_new.getRow(y_new[i]), sg_new.getColumn(x_new[j]), sg.getMatrix()[i/2][j/2]);
				}
				//i and j odd - old grid center -> calculate mean value of the 4 neighboring grid corners
				else if (i%2==1 && j%2==1){
					sg_new.setValue(sg_new.getRow(y_new[i]), sg_new.getColumn(x_new[j]), (sg.getMatrix()[i/2][j/2] + sg.getMatrix()[i/2][(j/2)+1] + sg.getMatrix()[(i/2)+1][j/2] + sg.getMatrix()[(i/2)+1][(j/2)+1])/4);
				}
				//either i or j even - value on old grid edge -> calculate mean value of the 2 neighboring grid corners
				else if (i%2==0){
					sg_new.setValue(sg_new.getRow(y_new[i]), sg_new.getColumn(x_new[j]), (sg.getMatrix()[i/2][j/2] + sg.getMatrix()[i/2][(j/2)+1])/2);
				}
				else{ //j%2==0
					sg_new.setValue(sg_new.getRow(y_new[i]), sg_new.getColumn(x_new[j]), (sg.getMatrix()[i/2][j/2] + sg.getMatrix()[(i/2)+1][j/2])/2);
				}
			}
		}
		return sg_new;
	}
	
	/**
	 * old implementation without SpatialGrid
	 * please use myBiLinearValueInterpolation(SpatialGrid sg, double xCoord, double yCoord)
	 * 
	 * interpolates the value on a arbitrary point with bilinear interpolation
	 * requires values on a grid as double[][]
	 * 
	 * @param values the known values on the grid
	 * @param xCoord the x-coordinate of the point to interpolate
	 * @param yCoord the y-coordinate of the point to interpolate
	 * @return interpolated value on the point (xCoord, yCoord)
	 */
	@Deprecated
	static double biLinearValueInterpolation(double[][] values, double xCoord, double yCoord){
		int x1= (int)xCoord;
		int x2= x1+1;
		int y1= (int)yCoord;
		int y2= y1+1;
		
		double xWeight= xCoord-x1;
		double yWeight= yCoord-y1;
		
		if (xWeight==0){
			if (yWeight==0){
				return values[y1][x1];
			}
			return values[y1][x1]*(1-yWeight) + values[y2][x1]*yWeight;
		}
		if (yWeight==0){
			return values[y1][x1]*(1-xWeight) + values[y1][x2]*xWeight;
		}
		
		return (values[y1][x1]*(1-yWeight) + values[y2][x1]*yWeight) * (1-xWeight) + (values[y1][x2]*(1-yWeight) + values[y2][x2]*yWeight) * xWeight;
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
	
	/**
	 * necessary for the deprecated method myBiLinearGridInterpolation(SpatialGrid sg)
	 * 
	 * creates a coordinate vector
	 * 
	 * @param min the minimum coordinate
	 * @param max the maximum coordinate
	 * @param resolution
	 * @return coordinate vector from min to max with the given resolution
	 */
	@Deprecated
	private static double[] coord(double min, double max, double resolution) {
		double[] coord = new double[(int) ((max - min) / resolution) + 1];
		coord[0] = min;
		for (int i = 1; i < coord.length; i++) {
			coord[i] = min + i * resolution;
		}
		return coord;
	}
}
