package org.matsim.contrib.map2mapmatching.gui.core;

import org.matsim.contrib.map2mapmatching.utils.geometry.Point2D;
import org.matsim.contrib.map2mapmatching.utils.geometry.Vector2D;

public class Camera2D implements Camera {

	//Constantes
	private static final double ZOOM_RATE = 5.0/4.0;

	//Attributes
	private final Point2D upLeftCorner;
	protected final Vector2D size;
	protected int width;
	protected int height;
	protected int frameSize=10;
	
	//Methods
	public Camera2D() {
		upLeftCorner = new Point2D(0, 1);
		size = new Vector2D(upLeftCorner, new Point2D(1, 0));
	}
	public double[] getCenter() {
		Point2D p = upLeftCorner.getTranslated(size.getScaled(0.5));
		return new double[]{p.getX(), p.getY()};
	}
	public double[] getUpLeftCorner() {
		return new double[]{upLeftCorner.getX(), upLeftCorner.getY()};
	}
	public double[] getSize() {
		return new double[]{size.getX(), size.getY()};
	}
	public double getAspectRatio() {
		return size.getX()/-size.getY();
	}
	public void copyCamera(Camera camera) {
		upLeftCorner.setX(camera.getUpLeftCorner()[0]);
		upLeftCorner.setY(camera.getUpLeftCorner()[1]);
		size.setX(camera.getSize()[0]);
		size.setY(camera.getSize()[1]);
	}
	public void copyCenter(Camera camera) {
		centerCamera(camera.getCenter());
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
	public void zoomIn(double x, double y) {
		Vector2D difCenters = new Vector2D(new Point2D(getCenter()[0], getCenter()[1]),new Point2D(x, y));
		Vector2D difCorners = new Vector2D(size.getX()*(1-1/ZOOM_RATE)/2,size.getY()*(1-1/ZOOM_RATE)/2);
		upLeftCorner.translate(difCenters);
		upLeftCorner.translate(difCorners);
		size.scale(1/ZOOM_RATE);
	}
	public void zoomOut(double x, double y) {
		Vector2D difCenters = new Vector2D(new Point2D(getCenter()[0], getCenter()[1]),new Point2D(x, y));
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
	public void move(int dx, int dy) {
		Vector2D difCorners = new Vector2D(getWorldDistance(dx), -getWorldDistance(dy));
		upLeftCorner.translate(difCorners);
	}
	public void move2(int dx, int dy) {
		
	}
	public void setFrameSize(int frameSize) {
		this.frameSize = frameSize;
	}
	public void centerCamera(double[] p) {
		upLeftCorner.translate(new Vector2D(new Point2D(getCenter()[0], getCenter()[1]),new Point2D(p[0], p[1])));
	}
	public int[] getScreenXY(double[] point) {
		return new int[]{(int)((point[0]-upLeftCorner.getX())*width/size.getX())+frameSize, (int)((point[1]-upLeftCorner.getY())*height/size.getY())+frameSize};
	}
	public double[] getWorld(int x, int y) {
		return new double[]{(x-frameSize)*size.getX()/width+upLeftCorner.getX(), (y-frameSize)*size.getY()/height+upLeftCorner.getY()};
	}
	public double setAspectRatio(int width, int height) {
		this.width = width-2*frameSize;
		this.height = height-2*frameSize;
		return ((double)this.width)/((double)this.height);
	}
	public double getWorldDistance(int d) {
		return d*size.getX()/width;
	}
	public boolean isInside(double[] p) {
		return upLeftCorner.getX()<p[0] && upLeftCorner.getY()+size.getY()<p[1] && upLeftCorner.getX()+size.getX()>p[0] && upLeftCorner.getY()>p[1];
	}

}
