package playground.sergioo.busRoutesVisualizer2011.gui;

import others.sergioo.util.geometry.Point2D;
import others.sergioo.util.geometry.Vector2D;

public class Camera {
	
	//Constantes
	private static final double ZOOM_RATE = 5.0/4.0;
	
	//Attributes
	private Point2D upLeftCorner;
	private Vector2D size;
	protected int width;
	protected int height;
	protected int frameSize=10;
	
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
	public void zoomIn(int x, int y) {
		Vector2D difCenters = new Vector2D(getCenter(),new Point2D(getDoubleX(x), getDoubleY(y)));
		Vector2D difCorners = new Vector2D(size.getX()*(1-1/ZOOM_RATE)/2,size.getY()*(1-1/ZOOM_RATE)/2);
		upLeftCorner.translate(difCenters);
		upLeftCorner.translate(difCorners);
		size.scale(1/ZOOM_RATE);
	}
	public void zoomOut(int x, int y) {
		Vector2D difCenters = new Vector2D(getCenter(),new Point2D(getDoubleX(x), getDoubleY(y)));
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
	public int getIntX(double x) {
		return (int)((x-upLeftCorner.getX())*width/size.getX())+frameSize;
	}
	public int getIntY(double y) {
		return (int)((y-upLeftCorner.getY())*height/size.getY())+frameSize;
	}
	public double getDoubleX(int x) {
		return (x-frameSize)*size.getX()/width+upLeftCorner.getX();
	}
	public double getDoubleY(int y) {
		return (y-frameSize)*size.getY()/height+upLeftCorner.getY();
	}
	public double setAspectRatio(int width, int height) {
		this.width = width-2*frameSize;
		this.height = height-2*frameSize;
		return ((double)this.width)/((double)this.height);
	}
	public void move(int x, int ix, int y, int iy) {
		Vector2D difCorners = new Vector2D(getDoubleX(ix)-getDoubleX(x),getDoubleX(y)-getDoubleX(iy));
		upLeftCorner.translate(difCorners);
	}
	public void centerCamera(double x, double y) {
		upLeftCorner.translate(new Vector2D(getCenter(),new Point2D(x, y)));
	}
}
