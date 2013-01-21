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

package playground.sergioo.visualizer2D2012.networkVisualizer;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.matsim.api.core.v01.network.Link;

import playground.sergioo.visualizer2D2012.Camera2D;
import playground.sergioo.visualizer2D2012.Camera3DOrtho;
import playground.sergioo.visualizer2D2012.Camera3DPersp;
import playground.sergioo.visualizer2D2012.Layer;
import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.LayersWindow;
import playground.sergioo.visualizer2D2012.networkVisualizer.SimpleNetworkWindow.Labels;
import playground.sergioo.visualizer2D2012.networkVisualizer.SimpleNetworkWindow.Options;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainterManager;

public class NetworkPanel extends LayersPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	protected final LayersWindow window;
	private int iniX;
	private int iniY;
	private int button;
	
	//Methods
	public NetworkPanel(LayersWindow window, NetworkPainter networkPainter) {
		super();
		this.window = window;
		addLayer(new Layer(networkPainter));
		this.setBackground(backgroundColor);
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
		double[] p = getWorld(e.getX(), e.getY());
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(p);
		else {
			if(window.getOption().equals(Options.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectLink(p[0], p[1]);
				window.refreshLabel(Labels.LINK);
			}
			else if(window.getOption().equals(Options.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().unselectLink();
				window.refreshLabel(Labels.LINK);
			}
			else if(window.getOption().equals(Options.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectNode(p[0], p[1]);
				window.refreshLabel(Labels.NODE);
			}
			else if(window.getOption().equals(Options.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().unselectNode();
				window.refreshLabel(Labels.NODE);
			}
			else if(window.getOption().equals(Options.ADD_LINK) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().addLink(p[0], p[1]);
				window.refreshLabel(Labels.LINK);
			}
			else if(window.getOption().equals(Options.ADD_LINK) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().removeLink(p[0], p[1]);
				window.refreshLabel(Labels.LINK);
			}
			else if(window.getOption().equals(Options.ADD_NODE) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().addNode(p[0], p[1]);
				window.refreshLabel(Labels.NODE);
			}
			else if(window.getOption().equals(Options.ADD_NODE) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().removeNode(p[0], p[1]);
				window.refreshLabel(Labels.NODE);
			}
			else if(window.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
				camera.zoomIn(p[0], p[1]);
			else if(window.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
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
		this.button = e.getButton();
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
		if(button==MouseEvent.BUTTON1) {
			camera.move(iniX-e.getX(), iniY-e.getY());
			iniX = e.getX();
			iniY = e.getY();
		}
		else {
			camera.move2(iniX-e.getX(), iniY-e.getY());
			iniX = e.getX();
			iniY = e.getY();
		}
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
			getActiveLayer().changeVisible();
			break;
		case 'o':
			((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager().selectOppositeLink();
			window.refreshLabel(Labels.LINK);
			break;
		case 'i':
			JFileChooser jFileChooser = new JFileChooser();
			jFileChooser.showSaveDialog(this);
			File file = jFileChooser.getSelectedFile();
			saveImage(file.getName().split("\\.")[file.getName().split("\\.").length-1], file, Integer.parseInt(JOptionPane.showInputDialog("Width", "12040")),  Integer.parseInt(JOptionPane.showInputDialog("Height", "6012")));
			break;
		case 'v':
			viewAll();
			break;
		case '2':
			this.setCamera(new Camera2D());
			viewAll();
			break;
		case '3':
			this.setCamera(new Camera3DOrtho());
			viewAll();
			break;
		case '4':
			this.setCamera(new Camera3DPersp());
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
