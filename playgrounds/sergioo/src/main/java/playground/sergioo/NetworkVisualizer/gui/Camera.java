package playground.sergioo.NetworkVisualizer.gui;

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
	public void setCamera(Point2D upLeftCorner, Vector2D size) {
		this.upLeftCorner = upLeftCorner;
		this.size = size;
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
		upLeftCorner = new Point2D(xMin, yMax);
		size = new Vector2D(upLeftCorner, new Point2D(xMax, yMin));
		double smallXSize = SimpleNetworkWindow.MAX_HEIGHT*size.getX()/-size.getY();
		double smallYSize = SimpleNetworkWindow.MAX_WIDTH*-size.getY()/size.getX();
		SimpleNetworkWindow.width=(int) (size.getX()/-size.getY()>(double)SimpleNetworkWindow.MAX_WIDTH/(double)SimpleNetworkWindow.MAX_HEIGHT?SimpleNetworkWindow.MAX_WIDTH:smallXSize<SimpleNetworkWindow.MIN_WIDTH?SimpleNetworkWindow.MIN_WIDTH:smallXSize);
		SimpleNetworkWindow.height=(int) (-size.getY()/size.getX()>(double)SimpleNetworkWindow.MAX_HEIGHT/(double)SimpleNetworkWindow.MAX_WIDTH?SimpleNetworkWindow.MAX_HEIGHT:smallYSize<SimpleNetworkWindow.MIN_HEIGHT?SimpleNetworkWindow.MIN_HEIGHT:smallYSize);
	}
	public int getIntX(double x) {
		return (int) ((x-upLeftCorner.getX())*(SimpleNetworkWindow.width-2*SimpleNetworkWindow.FRAMESIZE)/size.getX())+SimpleNetworkWindow.FRAMESIZE;
	}
	public int getIntY(double y) {
		return (int) ((y-upLeftCorner.getY())*(SimpleNetworkWindow.height-2*SimpleNetworkWindow.FRAMESIZE)/size.getY())+SimpleNetworkWindow.FRAMESIZE;
	}
	public double getDoubleX(int x) {
		return (x-SimpleNetworkWindow.FRAMESIZE)*size.getX()/(SimpleNetworkWindow.width-2*SimpleNetworkWindow.FRAMESIZE)+upLeftCorner.getX();
	}
	public double getDoubleY(int y) {
		return (y-SimpleNetworkWindow.FRAMESIZE)*size.getY()/(SimpleNetworkWindow.height-2*SimpleNetworkWindow.FRAMESIZE)+upLeftCorner.getY();
	}
	public void move(int x, int ix, int y, int iy) {
		Vector2D difCorners = new Vector2D(getDoubleX(ix)-getDoubleX(x),getDoubleX(y)-getDoubleX(iy));
		upLeftCorner.translate(difCorners);
	}
	public void centerCamera(double x, double y) {
		upLeftCorner.translate(new Vector2D(getCenter(),new Point2D(x, y)));
	}
}
