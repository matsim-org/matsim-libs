package playground.sergioo.Visualizer2D;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;

import org.matsim.api.core.v01.Coord;

public abstract class LayersPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	protected Color backgroundColor = Color.WHITE;
	protected final Camera camera;
	protected final List<Layer> layers;
	private int width;
	private int height;
	private double xMax;
	private double yMax;
	private double xMin;
	private double yMin;
	private int frameSize=20;
	
	//Methods
	public LayersPanel() {
		layers = new ArrayList<Layer>();
		camera = new Camera();
	}
	public Camera getCamera() {
		return camera;
	}
	public void setFrameSize(int frameSize) {
		this.frameSize = frameSize;
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
	}
	public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        setAspectRatio();
    }
	protected void setPreferredSize(double maxWidth, double maxHeight) {
		double aspectRatio = (xMax-xMin)/(yMax-yMin);
		double width = maxHeight*aspectRatio;
		double height = maxWidth/aspectRatio;
		int widthInt=(int) (aspectRatio>maxWidth/maxHeight?maxWidth:width);
		int heightInt=(int) (1/aspectRatio>maxHeight/maxWidth?maxHeight:height);
		setPreferredSize(new Dimension(widthInt, heightInt));
	}
	private void setAspectRatio() {
		width = this.getWidth()-2*frameSize;
		height = this.getHeight()-2*frameSize;
		double aspectRatioPanel = ((double)width)/((double)height);
		double aspectRatioWorld = (xMax-xMin)/(yMax-yMin);
		double cXMin = xMin, cYMin = yMin, cXMax = xMax, cYMax = yMax;
		if(aspectRatioWorld>aspectRatioPanel) {
			cXMin=xMin;
			cXMax=xMax;
			cYMin=yMin+(yMax-yMin)/2-((xMax-xMin)/aspectRatioPanel)/2;
			cYMax=yMax-(yMax-yMin)/2+((xMax-xMin)/aspectRatioPanel)/2;
		}
		else if(aspectRatioWorld<aspectRatioPanel){
			cYMin=yMin;
			cYMax=yMax;
			cXMin=xMin+(xMax-xMin)/2-((yMax-yMin)*aspectRatioPanel)/2;
			cXMax=xMax-(xMax-xMin)/2+((yMax-yMin)*aspectRatioPanel)/2;
		}
		camera.setBoundaries(cXMin, cYMin, cXMax, cYMax);
	}
	public void viewAll() {
		setAspectRatio();
	}
	public int getScreenX(double x) {
		return (int) ((x-camera.getUpLeftCorner().getX())*width/camera.getSize().getX())+(this.getWidth()-width)/2;
	}
	public int getScreenY(double y) {
		return (int) ((y-camera.getUpLeftCorner().getY())*height/camera.getSize().getY())+(this.getHeight()-height)/2;
	}
	public double getWorldX(int x) {
		return (x-(this.getWidth()-width)/2)*camera.getSize().getX()/width+camera.getUpLeftCorner().getX();
	}
	public double getWorldY(int y) {
		return (y-(this.getHeight()-height)/2)*camera.getSize().getY()/height+camera.getUpLeftCorner().getY();
	}
	
}
