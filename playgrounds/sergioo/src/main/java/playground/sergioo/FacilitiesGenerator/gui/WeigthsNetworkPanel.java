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
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import javax.imageio.ImageIO;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.utils.collections.Tuple;

import playground.sergioo.FacilitiesGenerator.gui.WeigthsNetworkWindow.Option;
import playground.sergioo.Visualizer2D.ArrowsPainter;
import playground.sergioo.Visualizer2D.Camera;
import playground.sergioo.Visualizer2D.Layer;
import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;

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
	public WeigthsNetworkPanel(WeigthsNetworkWindow window, NetworkPainter networkPainter, Map<Tuple<Id, Id>, Tuple<Boolean, Double>> ids, SortedMap<Id, ActivityFacility> mPAreas, SortedMap<String, Coord> stopsBase) {
		super();
		this.window = window;
		addLayer(new Layer(networkPainter));
		ArrowsPainter arrowsPainter = new ArrowsPainter();
		for(Entry<Tuple<Id, Id>, Tuple<Boolean, Double>> data:ids.entrySet()) {
			arrowsPainter.addLine(stopsBase.get(data.getKey().getFirst().toString()),mPAreas.get(data.getKey().getSecond()).getCoord());
			if(data.getValue().getFirst())
				arrowsPainter.addColor(new Color(data.getValue().getSecond().floatValue()*0.5f+0.5f,0,0));
			else
				arrowsPainter.addColor(new Color(data.getValue().getSecond().floatValue()*0.5f+0.5f,data.getValue().getSecond().floatValue()*0.25f+0.5f,0));
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
		Collection<Coord> coords = new ArrayList<Coord>();
		for(Link link:((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager().getNetworkLinks()) {
			if(link!=null) {
				coords.add(link.getFromNode().getCoord());
				coords.add(link.getToNode().getCoord());
			}
		}
		super.calculateBoundaries(coords);
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		this.requestFocus();
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(getWorldX(e.getX()), getWorldY(e.getY()));
		else {
			if(window.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
				camera.zoomIn(getWorldX(e.getX()), getWorldY(e.getY()));
			else if(window.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
				camera.zoomOut(getWorldX(e.getX()), getWorldY(e.getY()));
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
		camera.move(getWorldX(iniX)-getWorldX(e.getX()),getWorldY(iniY)-getWorldY(e.getY()));
		iniX = e.getX();
		iniY = e.getY();
		repaint();
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		window.setCoords(getWorldX(e.getX()),getWorldY(e.getY()));
	}
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if(e.getWheelRotation()<0)
			camera.zoomIn();
		else if(e.getWheelRotation()>0)
			camera.zoomOut();
		repaint();
	}
	private void saveImage() {
		Image windowImage = this.createImage(this.getSize().width, this.getSize().height);
		this.paintComponent(windowImage.getGraphics());
		try {
			ImageIO.write((RenderedImage) windowImage, "png", new File("./data/prueba.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Image saved");
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
		case 's':
			saveImage();
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
