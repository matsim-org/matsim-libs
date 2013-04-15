package interpolation_old;

import org.matsim.contrib.matsim4opus.gis.SpatialGrid;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Implements inverse distance weighting for interpolation. Own implementation (no suitable implementation found).
 * 
 * Requires values on a SpatialGrid.
 * 
 * Problem: Peaks and valleys may occur, if the user chooses unsuitable parameters (exponent of weights and number of sampling points to consider).
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
	 * Initiates the interpolation of the value on an arbitrary point with inverse distance weighting.
	 * Attention: Using shapefile data, the inverse distance weighting isn't correct on the boundary. Please use bounding box data.
	 * 
	 * @param xCoord the x-coordinate of the point to interpolate
	 * @param yCoord the y-coordinate of the point to interpolate
	 * @param allNeighbors sets, whether the inverse distance weighting with all or four neighbors should be used.
	 * @param exponent the exponent for the inverse distance weighting
	 * @return interpolated value on the point (xCoord, yCoord) 
	 */
	double inverseDistanceWeighting(double xCoord, double yCoord, boolean allNeighbors, double exponent){
		if (allNeighbors)
			return allValuesIDW(this.sg, xCoord, yCoord, exponent);
		else
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
		GeometryFactory factory = new GeometryFactory();
		//create dummy coordinates for the 4 nearest neighbors
		Point p1= factory.createPoint(new Coordinate(0,0));
		Point p2= factory.createPoint(new Coordinate(0,0));
		Point p3= factory.createPoint(new Coordinate(0,0)); 
		Point p4= factory.createPoint(new Coordinate(0,0));
		
		double xDif= (xCoord-sg.getXmin()) % sg.getResolution();
		double yDif= (yCoord-sg.getYmin()) % sg.getResolution();
		
		//find the 4 nearest neighbors
		if (xDif==0){
			if (yDif==0){//the point is a grid point, so the value is known
				return sg.getValue(xCoord, yCoord);
			}else{ //the point lies on a cell boundary parallel to the y-axis (only xDif=0)
				double x2= xCoord;
				double x1= x2-sg.getResolution();
				double x3= x2+sg.getResolution();
				double y1= yCoord-yDif;
				double y2= y1+sg.getResolution();
				p1= factory.createPoint(new Coordinate(x2, y1));
				p4= factory.createPoint(new Coordinate(x2, y2));
				if(yDif<sg.getResolution()/2){
					p3= factory.createPoint(new Coordinate(x1, y1));
					p2= factory.createPoint(new Coordinate(x3, y1));
				}
				else{ //TODO? Spezialfall betrachten: wenn yDif=res/2
					p3= factory.createPoint(new Coordinate(x1, y2));
					p2= factory.createPoint(new Coordinate(x3, y2));
				}
			}
		}
		else if (yDif==0){ //the point lies on a cell boundary parallel to the x-axis (only yDif=0)
			double y2= yCoord;
			double y1= y2-sg.getResolution();
			double y3= y2+sg.getResolution();
			double x1= xCoord-xDif;
			double x2= x1+sg.getResolution();
			p1= factory.createPoint(new Coordinate(x1, y2));
			p2= factory.createPoint(new Coordinate(x2, y2));
			if(xDif<sg.getResolution()/2){
				p3= factory.createPoint(new Coordinate(x1, y1));
				p4= factory.createPoint(new Coordinate(x1, y3));
			}
			else{ //TODO? Spezialfall betrachten: wenn yDif=res/2
				p3= factory.createPoint(new Coordinate(x2, y1));
				p4= factory.createPoint(new Coordinate(x2, y3));
			}
		}
		else{ //the point lies in a grid cell
			double x1= xCoord-xDif;
			double x2= x1+sg.getResolution();
			double y1= yCoord-yDif;
			double y2= y1+sg.getResolution();
			p1= factory.createPoint(new Coordinate(x1, y1));
			p2= factory.createPoint(new Coordinate(x2, y1));
			p3= factory.createPoint(new Coordinate(x2, y2));
			p4= factory.createPoint(new Coordinate(x1, y2));
		}
		
		//calculate distances to the 4 nearest neighbors
		double d_p1= Math.pow(distance(p1.getX(), p1.getY(), xCoord, yCoord), exp);
		double d_p2= Math.pow(distance(p2.getX(), p2.getY(), xCoord, yCoord), exp);
		double d_p3= Math.pow(distance(p3.getX(), p3.getY(), xCoord, yCoord), exp);
		double d_p4= Math.pow(distance(p4.getX(), p4.getY(), xCoord, yCoord), exp);
		
		//interpolation on the boundary
		if (xCoord == sg.getXmax() || xCoord == sg.getXmin()){
			//consider only 2 neighbors (up and down)
			return (sg.getValue(p1)/d_p1 + sg.getValue(p4)/d_p4) / (1/d_p1 + 1/d_p4);
		}
		if (yCoord == sg.getYmax() || yCoord == sg.getYmin()){
			//consider only 2 neighbors (left and right)
			return (sg.getValue(p1)/d_p1 + sg.getValue(p2)/d_p2) / (1/d_p1 + 1/d_p2);
		}
		
		//interpolation with 4 neighbors
		return (sg.getValue(p1)/d_p1 + sg.getValue(p2)/d_p2 + sg.getValue(p3)/d_p3 + sg.getValue(p4)/d_p4) 
				/ (1/d_p1 + 1/d_p2 + 1/d_p3 + 1/d_p4);
	}
	
	//TODO raus
//	static double sixteenNeighborsIDW(SpatialGrid sg, double xCoord, double yCoord, double exp) {
//		double xDif= (xCoord-sg.getXmin()) % sg.getResolution();
//		double yDif= (yCoord-sg.getYmin()) % sg.getResolution();
//		
//		//known value
//		if (xDif==0 && yDif==0){
//			return sg.getValue(xCoord, yCoord);
//		}
//		
//		double x1= xCoord-xDif-sg.getResolution();
//		double x2= x1+sg.getResolution();
//		double x3= x2+sg.getResolution();
//		double x4= x3+sg.getResolution();
//		double y1= yCoord-yDif-sg.getResolution();
//		double y2= y1+sg.getResolution();
//		double y3= y2+sg.getResolution();
//		double y4= y3+sg.getResolution();
//		
//		//calculate distances to the 16 neighbors
//		double d11= Math.pow(distance(x1, y1, xCoord, yCoord), exp);
//		double d12= Math.pow(distance(x1, y2, xCoord, yCoord), exp);
//		double d13= Math.pow(distance(x1, y3, xCoord, yCoord), exp);
//		double d14= Math.pow(distance(x1, y4, xCoord, yCoord), exp);
//		double d21= Math.pow(distance(x2, y1, xCoord, yCoord), exp);
//		double d22= Math.pow(distance(x2, y2, xCoord, yCoord), exp);
//		double d23= Math.pow(distance(x2, y3, xCoord, yCoord), exp);
//		double d24= Math.pow(distance(x2, y4, xCoord, yCoord), exp);
//		double d31= Math.pow(distance(x3, y1, xCoord, yCoord), exp);
//		double d32= Math.pow(distance(x3, y2, xCoord, yCoord), exp);
//		double d33= Math.pow(distance(x3, y3, xCoord, yCoord), exp);
//		double d34= Math.pow(distance(x3, y4, xCoord, yCoord), exp);
//		double d41= Math.pow(distance(x4, y1, xCoord, yCoord), exp);
//		double d42= Math.pow(distance(x4, y2, xCoord, yCoord), exp);
//		double d43= Math.pow(distance(x4, y3, xCoord, yCoord), exp);
//		double d44= Math.pow(distance(x4, y4, xCoord, yCoord), exp);
//		
////		//interpolation on the boundary
////		if (xCoord == sg.getXmax()){
////			//consider only 8 neighbors
////			return (sg.getValue(x1, y1)/d11 + sg.getValue(x1, y2)/d12) / (1/d11 + 1/d12); //TODO
////		}
////		if (yCoord == sg.getYmax()){
////			//consider only 8 neighbors
////			return (sg.getValue(x1, y1)/d11 + sg.getValue(x2, y1)/d21) / (1/d11 + 1/d21);
////		}
//		if (y4>sg.getYmax() || y1<sg.getYmin() || x1<sg.getXmin() || x4>sg.getXmax())
//			return 0; //TODO ausnahme behandeln
//		
//		//interpolation with 16 neighbors
//		return (sg.getValue(x1, y1)/d11 + sg.getValue(x1, y2)/d12 + sg.getValue(x1, y3)/d13 + sg.getValue(x1, y4)/d14 +
//				sg.getValue(x2, y1)/d21 + sg.getValue(x2, y2)/d22 + sg.getValue(x2, y3)/d23 + sg.getValue(x2, y4)/d24 +
//				sg.getValue(x3, y1)/d31 + sg.getValue(x3, y2)/d32 + sg.getValue(x3, y3)/d33 + sg.getValue(x3, y4)/d34 +
//				sg.getValue(x4, y1)/d41 + sg.getValue(x4, y2)/d42 + sg.getValue(x4, y3)/d43 + sg.getValue(x4, y4)/d44)
//				/ (1/d11 + 1/d12 + 1/d13 + 1/d14 + 1/d21 + 1/d22 + 1/d23 + 1/d24 + 1/d31 + 1/d32 + 1/d33 + 1/d34 + 1/d41 + 1/d42 + 1/d43 + 1/d44);
//	}
	
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
	 * Interpolates a value at the given point (xCoord, yCoord) with inverse distance weighting with variable power of weights.
	 * The interpolation considers all sampling points:
	 * z(u_0)= Sum((1/d_i^exp)*z(u_i)) / Sum (1/d_i^exp).
	 * Attention: This interpolation needs much more calculation time than the interpolation with consideration of only four neighbors.
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
		
		//known value
		if (xDif==0 && yDif==0){
			return sg.getValue(xCoord, yCoord);
		}
		
		if (Double.isNaN(sg.getValue(xCoord, yCoord))){
			return Double.NaN;
		}
		
		//interpolation with all neighbors
		double distanceSum=0;
		double currentWeight=1;
		double weightSum=0;
		for (double y = sg.getYmin(); y <= sg.getYmax(); y += sg.getResolution()){
			for (double x = sg.getXmin(); x <= sg.getXmax(); x += sg.getResolution()){
				//consider only the known values
				if(!Double.isNaN(sg.getValue(x, y))){
					currentWeight= Math.pow(distance(x, y, xCoord, yCoord), exp);
					distanceSum+= sg.getValue(x, y)/currentWeight;
					weightSum+= 1/currentWeight;
				}
			}
		}
		return distanceSum/weightSum;		
	}
	
}
