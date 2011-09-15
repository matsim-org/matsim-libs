package playground.sergioo.GTFS2PTSchedule.PathEditor.gui;

import util.geometry.Point2D;
import util.geometry.Vector2D;

public class Camera {
	
	//Constantes
	private static final double ZOOM_RATE = 5.0/4.0;
	
	//Attributes
	private Point2D upLeftCorner;
	private Vector2D size;
	
	//Methods
	public Point2D getCenter() {
		return upLeftCorner.getTranslated(size.getScaled(0.5));
	}
	public Point2D getUpLeftCorner() {
		return upLeftCorner;
	}
	public Vector2D getSize() {
		return size;
	}
	public void zoomIn() {
		Vector2D difCorners = new Vector2D(size.getX()*(1-1/ZOOM_RATE)/2,size.getY()*(1-1/ZOOM_RATE)/2);
		upLeftCorner.translate(difCorners);
		size.scale(1/ZOOM_RATE);
	}
	public void zoomOut() {
		Vector2D difCorners = new Vector2D(size.getX()*(1-ZOOM_RATE)/2,size.getY()*(1-ZOOM_RATE)/2);
		upLeftCorner.translate(difCorners);
		size.scale(ZOOM_RATE);
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
		upLeftCorner = new Point2D(xMin, yMax);
		size = new Vector2D(upLeftCorner, new Point2D(xMax, yMin));
		double smallXSize = Window.MAX_HEIGHT*size.getX()/-size.getY();
		double smallYSize = Window.MAX_WIDTH*-size.getY()/size.getX();
		Window.width=(int) (size.getX()/-size.getY()>(double)Window.MAX_WIDTH/(double)Window.MAX_HEIGHT?Window.MAX_WIDTH:smallXSize<Window.MIN_WIDTH?Window.MIN_WIDTH:smallXSize);
		Window.height=(int) (-size.getY()/size.getX()>(double)Window.MAX_HEIGHT/(double)Window.MAX_WIDTH?Window.MAX_HEIGHT:smallYSize<Window.MIN_HEIGHT?Window.MIN_HEIGHT:smallYSize);
	}
	public int getIntX(double x) {
		return (int) ((x-upLeftCorner.getX())*(Window.width-2*Window.FRAMESIZE)/size.getX())+Window.FRAMESIZE;
	}
	public int getIntY(double y) {
		return (int) ((y-upLeftCorner.getY())*(Window.height-2*Window.FRAMESIZE)/size.getY())+Window.FRAMESIZE;
	}
	public double getDoubleX(int x) {
		return (x-Window.FRAMESIZE)*size.getX()/(Window.width-2*Window.FRAMESIZE)+upLeftCorner.getX();
	}
	public double getDoubleY(int y) {
		return (y-Window.FRAMESIZE)*size.getY()/(Window.height-2*Window.FRAMESIZE)+upLeftCorner.getY();
	}
	public void move(int x, int ix, int y, int iy) {
		Vector2D difCorners = new Vector2D(getDoubleX(ix)-getDoubleX(x),getDoubleX(y)-getDoubleX(iy));
		upLeftCorner.translate(difCorners);
	}
	public void centerCamera(double x, double y) {
		upLeftCorner.translate(new Vector2D(getCenter(),new Point2D(x, y)));
	}
}
