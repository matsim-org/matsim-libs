package org.matsim.contrib.map2mapmatching.gui.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public abstract class LayersPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	protected Color backgroundColor = Color.WHITE;
	protected Camera camera;
	private final List<Layer> layers;
	private double xMax;
	private double yMax;
	private double xMin;
	private double yMin;
	protected byte activeLayer = -1;
	protected byte principalLayer = -1;
	private int yAnt;
	private int heightAnt;
	private int xAnt;
	private int widthAnt;
	//Methods
	public LayersPanel() {
		layers = new ArrayList<Layer>();
		camera = new Camera2D();
	}
	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
        if(!(xAnt==x && yAnt==y && widthAnt==width && heightAnt==height)) {
        	xAnt = getX(); yAnt = getY(); widthAnt = getWidth(); heightAnt = getHeight();
        	setAspectRatio();
        }
    }
	public Camera getCamera() {
		return camera;
	}
	public void setCamera(Camera camera) {
		this.camera = camera;
	}
	public void centerCamera(double[] coord) {
		camera.centerCamera(coord);
	}
	protected Collection<Layer> getAllLayers() {
		return layers;
	}
	protected int getNumLayers() {
		return layers.size();
	}
	protected void addLayer(Layer layer) {
		if(principalLayer==-1)
			principalLayer = (byte) layers.size();
		if(activeLayer==-1 && layer.isActive())
			activeLayer = (byte) layers.size();
		layers.add(layer);
	}
	protected void addLayer(Layer layer, boolean principal) {
		if(principal)
			principalLayer = (byte) layers.size();
		if(activeLayer==-1 && layer.isActive())
			activeLayer = (byte) layers.size();
		layers.add(layer);
	}
	protected void addLayer(int position, Layer layer) {
		if(principalLayer==-1)
			principalLayer = (byte) position;
		if(activeLayer==-1 && layer.isActive())
			activeLayer = (byte) position;
		layers.add(position, layer);
	}
	protected void setLayer(int position, Layer layer) {
		layers.set(position, layer);
	}
	public Layer getLayer(int position) {
		return layers.get(position);
	}
	protected Layer removeFirstLayer() {
		return layers.remove(0);
	}
	protected Layer removeLastLayer() {
		return layers.remove(layers.size()-1);
	}
	protected Layer getActiveLayer() {
		return layers.get(activeLayer);
	}
	protected void changeActiveLayer() {
		do {
			activeLayer++;
		} while(activeLayer<layers.size() && !layers.get(activeLayer).isActive());
		if(activeLayer>=layers.size()) {
			activeLayer = 0;
			while(!layers.get(activeLayer).isActive())
				activeLayer++;
		}
	}
	protected Layer getPrincipalLayer() {
		return layers.get(principalLayer);
	}
	protected void swapLayers(int positionA, int positionB) {
		Layer temporalLayer = layers.get(positionA);
		layers.set(positionA, layers.get(positionB));
		layers.set(positionB, temporalLayer);
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
	protected void calculateBoundaries(Collection<double[]> coords) {
		xMin=Double.POSITIVE_INFINITY; yMin=Double.POSITIVE_INFINITY; xMax=Double.NEGATIVE_INFINITY; yMax=Double.NEGATIVE_INFINITY;
		for(double[] coord:coords) {
			double x = coord[0];
			double y = coord[1];
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
	protected void setPreferredSize(double maxWidth, double maxHeight) {
		double aspectRatio = (xMax-xMin)/(yMax-yMin);
		double width = maxHeight*aspectRatio;
		double height = maxWidth/aspectRatio;
		int widthInt=(int) (aspectRatio>maxWidth/maxHeight?maxWidth:width);
		int heightInt=(int) (1/aspectRatio>maxHeight/maxWidth?maxHeight:height);
		setPreferredSize(new Dimension(widthInt, heightInt));
	}
	void setAspectRatio() {
		double aspectRatioPanel = camera.setAspectRatio(getWidth(), getHeight());
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
	protected void saveImage(String type, File file) {
		Image windowImage = this.createImage(this.getSize().width, this.getSize().height);
		this.paintComponent(windowImage.getGraphics());
		try {
			ImageIO.write((RenderedImage) windowImage, type, file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Image saved");
	}
	protected void saveImage(String type, File file, int width, int height) {
		int prevWidth = this.getSize().width, prevHeight = this.getSize().height;
		Image windowImage = this.createImage(width, height);
		Camera camera = new Camera3DPersp();
		camera.copyCamera(this.camera);
		this.setSize(new Dimension(width, height));
		this.camera.copyCamera(camera);
		this.paintComponent(windowImage.getGraphics());
		try {
			ImageIO.write((RenderedImage) windowImage, type, file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.setSize(new Dimension(prevWidth, prevHeight));
		this.camera.copyCamera(camera);
		System.out.println("Image saved");
	}
	public void viewAll() {
		setAspectRatio();
	}
	public int[] getScreenXY(double[] point) {
		return camera.getScreenXY(point);
	}
	public double[] getWorld(int x, int y) {
		return camera.getWorld(x, y);
	}
	public double getWorldDistance(int d) {
		return camera.getWorldDistance(d);
	}

}
