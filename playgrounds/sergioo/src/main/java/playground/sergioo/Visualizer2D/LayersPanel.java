package playground.sergioo.Visualizer2D;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;

import org.matsim.api.core.v01.Coord;

import util.geometry.Point2D;

public abstract class LayersPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	protected final Camera camera;
	protected final List<Layer> layers;
	private double xMax;
	protected double yMax;
	protected double xMin;
	protected double yMin;
	private int frameSize=100;
	
	//Methods
	public LayersPanel() {
		layers = new ArrayList<Layer>();
		camera = new Camera();
	}
	public Camera getCamera() {
		return camera;
	}
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2=(Graphics2D)g;
		for(Layer layer:layers)
			try {
				layer.paint(g2, this);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	public void zoomIn(double x, double y) {
		camera.zoomIn(x, y);
	}
	public void zoomOut(double x, double y) {
		camera.zoomOut(x, y);
	}
	public void centerCamera(double x, double y) {
		camera.centerCamera(x, y);
	}
	public Point2D getCenter() {
		return camera.getCenter();
	}
	protected void calculateBoundaries(Collection<Coord> coords) {
		xMin=Double.POSITIVE_INFINITY; yMin=Double.POSITIVE_INFINITY; xMax=Double.NEGATIVE_INFINITY; yMax=Double.NEGATIVE_INFINITY;
		for(Coord coord:coords) {
			double x = coord.getX();
			double y = coord.getY();
			if(x<xMin)
				xMin = x;
			if(x>xMax)
				xMax = x;
			if(y<yMin)
				yMin = y;
			if(y>yMax)
				yMax = y;
		}
		viewAll();
	}
	protected void setSize(double aspectRatio, double maxWidth, double maxHeight) {
		double width = maxHeight*aspectRatio;
		double height = maxWidth*(1/aspectRatio);
		int widthInt=(int) (aspectRatio>maxWidth/maxHeight?maxWidth:width);
		int heightInt=(int) (1/aspectRatio>maxHeight/maxWidth?maxHeight:height);
		setPreferredSize(new Dimension(widthInt, heightInt));
	}
	protected void viewAll() {
		camera.setBoundaries(xMin, yMin, xMax, yMax);
	}
	public int getIntX(double x) {
		return (int) ((x-camera.getUpLeftCorner().getX())*(this.getWidth()-2*frameSize)/camera.getSize().getX())+frameSize;
	}
	public int getIntY(double y) {
		return (int) ((y-camera.getUpLeftCorner().getY())*(this.getHeight()-2*frameSize)/camera.getSize().getY())+frameSize;
	}
	public double getDoubleX(int x) {
		return (x-frameSize)*camera.getSize().getX()/(this.getWidth()-2*frameSize)+camera.getUpLeftCorner().getX();
	}
	public double getDoubleY(int y) {
		return (y-frameSize)*camera.getSize().getY()/(this.getHeight()-2*frameSize)+camera.getUpLeftCorner().getY();
	}
	
}
