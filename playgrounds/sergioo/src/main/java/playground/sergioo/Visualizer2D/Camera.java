package playground.sergioo.Visualizer2D;

import util.geometry.Point2D;
import util.geometry.Vector2D;

public class Camera {
	
	//Constantes
	private static final double ZOOM_RATE = 5.0/4.0;
	
	//Attributes
	private final Point2D upLeftCorner;
	private final Vector2D size;
	
	//Methods
	public Camera() {
		upLeftCorner = new Point2D(0, 1);
		size = new Vector2D(upLeftCorner, new Point2D(1, 0));
	}
	public Point2D getCenter() {
		return upLeftCorner.getTranslated(size.getScaled(0.5));
	}
	public Point2D getUpLeftCorner() {
		return upLeftCorner;
	}
	public Vector2D getSize() {
		return size;
	}
	public double getAspectRatio() {
		return size.getX()/-size.getY();
	}
	public void setCamera(Point2D upLeftCorner, Vector2D size) {
		this.upLeftCorner.setX(upLeftCorner.getX());
		this.upLeftCorner.setY(upLeftCorner.getY());
		this.size.setX(size.getX());
		this.size.setY(size.getY());
	}
	public void zoomIn(double x, double y) {
		Vector2D difCenters = new Vector2D(getCenter(),new Point2D(x, y));
		Vector2D difCorners = new Vector2D(size.getX()*(1-1/ZOOM_RATE)/2,size.getY()*(1-1/ZOOM_RATE)/2);
		upLeftCorner.translate(difCenters);
		upLeftCorner.translate(difCorners);
		size.scale(1/ZOOM_RATE);
	}
	public void zoomOut(double x, double y) {
		Vector2D difCenters = new Vector2D(getCenter(),new Point2D(x, y));
		Vector2D difCorners = new Vector2D(size.getX()*(1-ZOOM_RATE)/2,size.getY()*(1-ZOOM_RATE)/2);
		upLeftCorner.translate(difCenters);
		upLeftCorner.translate(difCorners);
		size.scale(ZOOM_RATE);
	}
	public void setBoundaries(double xMin, double yMin, double xMax, double yMax) {
		this.upLeftCorner.setX(xMin);
		this.upLeftCorner.setY(yMax);
		this.size.setX(xMax-xMin);
		this.size.setY(yMin-yMax);
	}
	public void move(double x, double ix, double y, double iy) {
		Vector2D difCorners = new Vector2D(ix-x,iy-y);
		upLeftCorner.translate(difCorners);
	}
	public void centerCamera(double x, double y) {
		upLeftCorner.translate(new Vector2D(getCenter(),new Point2D(x, y)));
	}
	
}
