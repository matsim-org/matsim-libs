package matsimConnector.utility;

import org.matsim.api.core.v01.Coord;

import pedCA.environment.grid.GridPoint;

public class MathUtility {
	public static double EuclideanDistance(Coord c1, Coord c2) {
		return Math.sqrt(Math.pow(c1.getX()-c2.getX(),2)+Math.pow(c1.getY()-c2.getY(),2));
	}
	
	public static void rotate(GridPoint point, double degrees){
		rotate(point, degrees, 0, 0);
	}
	
	public static void rotate(GridPoint point, double degrees, double x_center, double y_center){
		double x = point.getX();
		double y = point.getY();
		degrees = Math.toRadians(degrees);
		
		double x_res = Math.round(((x - x_center) * Math.cos(degrees)) - ((y - y_center) * Math.sin(degrees)) + x_center);
		double y_res = Math.round(((y - y_center) * Math.cos(degrees)) + ((x - x_center) * Math.sin(degrees)) + y_center);
		
		point.setX((int)x_res);
	    point.setY((int)y_res);
	}
	
	public static double convertGridCoordinate(int monodimCoord){
		return (monodimCoord+0.5)*Constants.CA_CELL_SIDE;
	}
	
	public static GridPoint gridPointDifference(GridPoint gp1, GridPoint gp2){
		return new GridPoint(gp1.getX()-gp2.getX(), gp1.getY()-gp2.getY());
	}
	
	public static GridPoint gridPointSum(GridPoint gp1, GridPoint gp2){
		return new GridPoint(gp1.getX()+gp2.getX(), gp1.getY()+gp2.getY());
	}
	
	public static double average(double x1, double x2){
		return (x1+x2)/2;
	}
}
