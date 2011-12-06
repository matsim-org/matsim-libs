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

package playground.sergioo.FacilitiesGenerator.gui;

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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.sergioo.FacilitiesGenerator.hits.PointPerson;
import playground.sergioo.Visualizer2D.Layer;
import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.LayersWindow;
import playground.sergioo.Visualizer2D.PointsPainter;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainterManager;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;
import playground.sergioo.Visualizer2D.NetworkVisualizer.SimpleNetworkWindow.Options;
import util.algebra.PointND;
import util.clustering.Cluster;

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
				pointsPainter.addPoint(new CoordImpl(point.getElement(0), point.getElement(1)));
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
	public ClustersPanel(ClustersWindow window, List<org.apache.commons.math.stat.clustering.Cluster<PointPerson>> clusters,	int numTotalPoints) {
		super();
		this.window = window;
		for(org.apache.commons.math.stat.clustering.Cluster<PointPerson> cluster:clusters) {
			Color color = new Color((float)(Math.random()*0.5), (float)(Math.random()*0.5), (float)(Math.random()*0.5));
			//Color color = new Color(5.5f*(float)(cluster.getPoints().size())/numTotalPoints, 5.5f*(float)(cluster.getPoints().size())/numTotalPoints, 5.5f*(float)(cluster.getPoints().size())/numTotalPoints);
			PointsPainter pointsPainter = new PointsPainter(color);
			for(PointPerson point:cluster.getPoints())
				pointsPainter.addPoint(new CoordImpl(point.getElement(0), point.getElement(1)));
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
	private void calculateBoundaries() {
		Collection<Coord> coords = new ArrayList<Coord>();
		for(int i=0; i<getNumLayers(); i++)
			for(Coord point:((PointsPainter)getLayer(i).getPainter()).getPoints())
				coords.add(point);
		super.calculateBoundaries(coords);
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(getWorldX(e.getX()), getWorldY(e.getY()));
		else {
			if(window.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
				camera.zoomIn(getWorldX(e.getX()), getWorldY(e.getY()));
			else if(window.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
				camera.zoomOut(getWorldX(e.getX()), getWorldY(e.getY()));
		}
		repaint();
	}
	public String getLabelText(playground.sergioo.Visualizer2D.LayersWindow.Labels label) {
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
		camera.move(getWorldX(iniX)-getWorldX(e.getX()),getWorldY(iniY)-getWorldY(e.getY()));
		iniX = e.getX();
		iniY = e.getY();
		repaint();
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		window.setCoords(getWorldX(e.getX())/3600,getWorldY(e.getY())/3600);
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
