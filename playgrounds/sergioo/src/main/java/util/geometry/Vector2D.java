package util.geometry;

/**
 * @author Sergio Ordóñez
 */
public class Vector2D implements Comparable<Vector2D> {
	
	//Attributes
	private double x;
	private double y;
	
	//Methods
	public Vector2D() {
		x = 0;
		y = 0;
	}
	public Vector2D(double x, double y) {
		super();
		this.x = x;
		this.y = y;
	}
	public Vector2D(double d, double t, boolean polar) {
		super();
		x = d*Math.cos(t);
		y = d*Math.sin(t);
	}
	public Vector2D(Point2D pi, Point2D pf) {
		super();
		x = pf.getX()-pi.getX();
		y = pf.getY()-pi.getY();
	}
	public Vector2D(Point2D point) {
		x=point.getX();
		y=point.getY();
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getMagnitude() {
		return Math.hypot(x,y);
	}
	public double getAngle() {
		return Math.atan2(y,x);
	}
	public double getDistance(Vector2D p2) {
		return Math.hypot(x-p2.getX(),y-p2.getY());
	}
	public double getDistanceSqr(Vector2D p2) {
		return Math.pow(x-p2.getX(),2)+Math.pow(y-p2.getY(),2);
	}
	public int compareTo(Vector2D o) {
		if(o.getY()==y && o.getX()==x)
			return 0;
		else if(this.getMagnitude()>o.getMagnitude())
			return 1;
		else
			return -1;
	}
	public void opposite() {
		x=-x;
		y=-y;
	}
	public Vector2D getOpposite() {
		return new Vector2D(-x, -y);
	}
	public Vector2D getUnit() {
		double m=this.getMagnitude();
		return new Vector2D(x/m, y/m);
	}
	public void setUnit() {
		double m=this.getMagnitude();
		x/=m;
		y/=m;
	}
	public void scale(double val) {
		x*=val;
		y*=val;
	}
	public Vector2D getScaled(double val) {
		return new Vector2D(x*val, y*val);
	}
	public void rotate(double ang) {
		double xr=x*Math.cos(ang)-y*Math.sin(ang);
		y=x*Math.sin(ang)+y*Math.cos(ang);
		x=xr;
	}
	public Vector2D getRotated(double ang) {
		double xr=x*Math.cos(ang)-y*Math.sin(ang);
		double yr=x*Math.sin(ang)+y*Math.cos(ang);
		return new Vector2D(xr, yr);
	}
	public double dotProduct(Vector2D vector2D) {
		return x*vector2D.x+y*vector2D.y;
	}
	public double crossProduct(Vector2D vector2D) {
		return x*vector2D.y-y*vector2D.x;
	}
	public void sum(Vector2D vector2D) {
		x+=vector2D.getX();
		y+=vector2D.getY();
	}
	public Vector2D getSum(Vector2D vector2D) {
		return new Vector2D(x+vector2D.getX(), y+vector2D.getY());
	}
	public double getAngleTo(Vector2D vector2D) {
		return Math.acos(this.dotProduct(vector2D)/(this.getMagnitude()*vector2D.getMagnitude()));
	}
}
