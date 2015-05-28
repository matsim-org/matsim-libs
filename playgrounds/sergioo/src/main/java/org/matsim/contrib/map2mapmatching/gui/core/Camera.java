package org.matsim.contrib.map2mapmatching.gui.core;

public interface Camera {

	public double[] getCenter();
	public double[] getUpLeftCorner();
	public double[] getSize();
	public double getAspectRatio();
	public void copyCamera(Camera camera);
	public void copyCenter(Camera camera);
	public void zoomIn();
	public void zoomOut();
	public void zoomIn(double x, double y);
	public void zoomOut(double x, double y);
	public void setBoundaries(double xMin, double yMin, double xMax, double yMax);
	public void move(int dx, int dy);
	public void move2(int dx, int dy);
	public void setFrameSize(int frameSize);
	public void centerCamera(double[] p);
	public int[] getScreenXY(double[] point);
	public double[] getWorld(int x, int y);
	public double setAspectRatio(int width, int height);
	public double getWorldDistance(int d);
	public boolean isInside(double[] p);

}
