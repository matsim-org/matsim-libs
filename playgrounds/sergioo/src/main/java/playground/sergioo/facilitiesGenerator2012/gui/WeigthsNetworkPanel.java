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

package playground.sergioo.facilitiesGenerator2012.gui;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import javax.swing.JFileChooser;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.sergioo.facilitiesGenerator2012.gui.WeigthsNetworkWindow.Option;
import playground.sergioo.visualizer2D2012.ArrowsPainter;
import playground.sergioo.visualizer2D2012.Camera;
import playground.sergioo.visualizer2D2012.Layer;
import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;

public class WeigthsNetworkPanel extends LayersPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private final WeigthsNetworkWindow window;
	private int iniX;
	private int iniY;
	private Color backgroundColor = Color.WHITE;
	private boolean withNetwork = true;
	
	//Methods
	public WeigthsNetworkPanel(WeigthsNetworkWindow window, NetworkPainter networkPainter, Map<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>, Tuple<Boolean, Double>> ids, SortedMap<Id<ActivityFacility>, ActivityFacility> mPAreas, SortedMap<String, Coord> stopsBase) {
		super();
		this.window = window;
		addLayer(new Layer(networkPainter));
		ArrowsPainter arrowsPainter = new ArrowsPainter();
		for(Entry<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>, Tuple<Boolean, Double>> data:ids.entrySet()) {
			if(data.getValue().getSecond()>0.01) {
				Coord coord = mPAreas.get(data.getKey().getSecond()).getCoord();
				double[] point = new double[]{coord.getX(), coord.getY()};
				coord = stopsBase.get(data.getKey().getFirst().toString());
				double[] pointS = new double[]{coord.getX(), coord.getY()};
				arrowsPainter.addLine(pointS, point);
				float scale = 50;
				if(data.getValue().getFirst())
					arrowsPainter.addColor(new Color(-0.5f*(new Double(Math.exp(-scale*data.getValue().getSecond()))).floatValue()+1f,0,0));
				else
					arrowsPainter.addColor(new Color(-0.5f*(new Double(Math.exp(-scale*data.getValue().getSecond()))).floatValue()+1f,-0.5f*(new Double(Math.exp(-scale*data.getValue().getSecond()))).floatValue()+1f,0));
			}
		}
		addLayer(new Layer(arrowsPainter));
		this.setBackground(backgroundColor);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	public Camera getCamera() {
		return camera;
	}
	private void calculateBoundaries() {
		Collection<double[]> coords = new ArrayList<double[]>();
		for(Link link:((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager().getNetworkLinks()) {
			if(link!=null) {
				coords.add(new double[]{link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()});
				coords.add(new double[]{link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY()});
			}
		}
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
	@Override
	public void mousePressed(MouseEvent e) {
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
		window.setCoords(p[0], p[1]);
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
		case 'n':
			withNetwork  = !withNetwork;
			break;
		case 'v':
			viewAll();
			break;
		case 'i':
			JFileChooser jFileChooser = new JFileChooser();
			jFileChooser.showSaveDialog(this);
			File file = jFileChooser.getSelectedFile();
			saveImage(file.getName().split("\\.")[file.getName().split("\\.").length-1], file);
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
