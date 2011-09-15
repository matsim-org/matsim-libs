package util.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergio Ordóñez
 */
public class Polyline2D {
	
	//Attributes
	/**
	 * The points of the two-dimensional Polygon
	 */
	private List<Point2D> points;
	
	//Methods
	/**
     * Creates a new empty polygon
     */
    public Polyline2D () {
    	points = new ArrayList<Point2D>();
    }
	/**
     * Creates a new polygon according to a list of points 
     * @param points the list of points. points.size()>=3
     */
    public Polyline2D (List<Point2D> points) {
    	this.points = points;
    }
    /**
     * Creates a new polygon according to a list of coordinates
     * <b>pre: </b> xs.length=ys.length
     * @param xs x coordinates. xs.length>=3
     * @param ys y coordinates. ys.length>=3
     */
    public Polyline2D (double[] xs, double[] ys) {
    	points = new ArrayList<Point2D>();
    	for(int i=0; i<xs.length; i++)
    		points.add(new Point2D(xs[i], ys[i]));
    }
    /**
     * Add a point to the end of the polygon  
     * @param point The two-dimensional new point. point!=null
     */
    public void addPoint(Point2D point) {
    	points.add(point);
    }
    /**  
     * @param pos The position of the point. pos>=0 && pos<points.size()
     * @return the required point
     */
    public Point2D getPoint(int pos) {
    	return points.get(pos);
    }
    /**
     * @return the number of points of the polygon
     */
    public int getNumPoints() {
		return points.size();
	}
	/**
	 * Transform the polygon information in a text
	 * @return The text
	 */
	public String toString() {
		String ps="";
		for(Point2D p:points)
			ps+=p+";";
		return ps;
	}
	
}
