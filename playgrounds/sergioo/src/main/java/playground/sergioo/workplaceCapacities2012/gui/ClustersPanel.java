/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.sergioo.workplaceCapacities2012.gui;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.matsim.api.core.v01.Coord;

import playground.sergioo.facilitiesGenerator2012.gui.WeigthsNetworkWindow.Option;
import playground.sergioo.visualizer2D2012.AxisPainter;
import playground.sergioo.visualizer2D2012.Layer;
import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.LayersWindow;
import playground.sergioo.visualizer2D2012.PointsPainter;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainterManager;
import playground.sergioo.workplaceCapacities2012.hits.PointPerson;
import others.sergioo.util.algebra.PointND;
import others.sergioo.util.clustering.Cluster;
import others.sergioo.visUtils.JetColor;

public class ClustersPanel extends LayersPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	protected final LayersWindow window;
	private int iniX;
	private int iniY;
	
	//Methods
	public ClustersPanel(LayersWindow window, Map<Integer,Cluster<Double>> clusters, int numTotalPoints) {
		super();
		this.window = window;
		for(Cluster<Double> cluster:clusters.values()) {
			Color color = new Color((float)(Math.random()*0.5), (float)(Math.random()*0.5), (float)(Math.random()*0.5));
			//Color color = new Color(5f*(float)(cluster.getPoints().size())/numTotalPoints, 5f*(float)(cluster.getPoints().size())/numTotalPoints, 5f*(float)(cluster.getPoints().size())/numTotalPoints);
			PointsPainter pointsPainter = new PointsPainter(color);
			for(PointND<Double> point:cluster.getPoints())
				pointsPainter.addPoint(new Coord(point.getElement(0), point.getElement(1)));
			addLayer(new Layer(pointsPainter));
		}
		this.setBackground(Color.CYAN);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	public ClustersPanel(ClustersWindow window, List<CentroidCluster<PointPerson>> clusters) {
		super();
		this.window = window;
		addLayer(new Layer(new AxisPainter(24*3600, 18*3600, 0, 6*3600, 3600, 3600, Color.DARK_GRAY)));
		PointsPainter pointsPainter = new PointsPainter();
		float i=0;
		for(org.apache.commons.math3.ml.clustering.Cluster<PointPerson> cluster:clusters) {
			//Color color = new Color((float)(Math.random()*0.5), (float)(Math.random()*0.5), (float)(Math.random()*0.5), 0.3f);
			float p = (i+0.5f)/clusters.size();
			Color color = JetColor.getJetColor(p);
			color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
			PointsPersonPainter ppPainter = new PointsPersonPainter(color);
			ppPainter.setWeightedPoints(cluster.getPoints());
			addLayer(new Layer(ppPainter));
			PointPerson center = cluster.getPoints().get(0).centroidOf(cluster.getPoints());
			pointsPainter.addPoint(new Coord(center.getElement(0), center.getElement(1)));
			i++;
		}
		addLayer(new Layer(pointsPainter));
		this.setBackground(Color.WHITE);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	private void calculateBoundaries() {
		Collection<double[]> coords = new ArrayList<double[]>();
		for(int i=1; i<getNumLayers()-1; i++)
			for(PointPerson point:((PointsPersonPainter)getLayer(i).getPainter()).getPoints())
				coords.add(new double[]{point.getElement(0),point.getElement(1)});
		coords.add(new double[]{-3600, 5*3600});
		coords.add(new double[]{25*3600, 19*3600});
		super.calculateBoundaries(coords);
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		this.requestFocus();
		double[] p = getWorld(e.getX(), e.getY());
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(p);
		else {
			if(window.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
				camera.zoomIn(p[0], p[1]);
			else if(window.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
				camera.zoomOut(p[0], p[1]);
		}
		repaint();
	}
	public String getLabelText(playground.sergioo.visualizer2D2012.LayersWindow.Labels label) {
		try {
			return (String) NetworkPainterManager.class.getMethod("refresh"+label.getText(), new Class[0]).invoke(((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager(), new Object[0]);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return "";
	}
	@Override
	public void mousePressed(MouseEvent e) {
		this.requestFocus();
		iniX = e.getX();
		iniY = e.getY();
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		
	}
	@Override
	public void mouseExited(MouseEvent e) {
		
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		camera.move(iniX-e.getX(), iniY-e.getY());
		iniX = e.getX();
		iniY = e.getY();
		repaint();
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		double[] p = getWorld(e.getX(), e.getY());
		window.setCoords(p[0]/3600,p[1]/3600);
	}
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if(e.getWheelRotation()<0)
			camera.zoomIn();
		else if(e.getWheelRotation()>0)
			camera.zoomOut();
		repaint();
	}
	@Override
	public void keyTyped(KeyEvent e) {
		switch(e.getKeyChar()) {
		case 'v':
			viewAll();
			break;
		}
		repaint();
	}
	@Override
	public void keyPressed(KeyEvent e) {
		
	}
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	
}
