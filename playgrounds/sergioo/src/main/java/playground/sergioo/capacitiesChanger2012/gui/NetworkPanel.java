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

package playground.sergioo.capacitiesChanger2012.gui;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.xml.sax.SAXException;

import others.sergioo.addressLocator2011.AddressLocator;
import others.sergioo.addressLocator2011.BadAddressException;

import playground.sergioo.capacitiesChanger2012.gui.SimpleNetworkWindow.Labels;
import playground.sergioo.capacitiesChanger2012.gui.SimpleNetworkWindow.Options;
import playground.sergioo.visualizer2D2012.Layer;
import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.LayersWindow;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;

public class NetworkPanel extends LayersPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	protected final LayersWindow window;
	private int iniX;
	private int iniY;
	private int posLocation = 0;
	private AddressLocator addressLocator;
	
	//Methods
	public NetworkPanel(LayersWindow window, NetworkPainter networkPainter) {
		super();
		addressLocator = new AddressLocator(TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N));
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
	public void findAddress() {
		requestFocus();
		try {
			addressLocator.locate(JOptionPane.showInputDialog("Insert the desired address")+" Singapore");
			posLocation = 0;
			if(addressLocator.getNumResults()>1)
				JOptionPane.showMessageDialog(this, "Many results: "+addressLocator.getNumResults()+".");
			try {
				JOptionPane.showMessageDialog(this, addressLocator.getLocation(posLocation).toString());
				Coord c = addressLocator.getLocation(posLocation);
				centerCamera(new double[]{c.getX(), c.getY()});
			} catch (HeadlessException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (BadAddressException e) {
			JOptionPane.showMessageDialog(this, "No results");
		}
	}
	public void selectLink(String id) {
		Link link = ((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectLink(id);
		if(link!=null)
			centerCamera(new double[]{link.getCoord().getX(), link.getCoord().getY()});
	}
	public void selectNode(String id) {
		Node node = ((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectNode(id);
		if(node!=null)
			centerCamera(new double[]{node.getCoord().getX(), node.getCoord().getY()});
	}
	public void saveNetwork(File file) {
		new NetworkWriter(((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager().getNetwork()).write(file.getAbsolutePath());
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		double[] p = getWorld(e.getX(), e.getY());
		if(e.getClickCount()==2) {
			if(window.getOption().equals(Options.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectLink(p[0], p[1]);
				Link link = ((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().getSelectedLink();
				try {
					link.setCapacity(Double.parseDouble(JOptionPane.showInputDialog("New capacity", link.getCapacity())));
				} catch(Exception e2) {
					
				}
				((SimpleSelectionNetworkPainter)getActiveLayer().getPainter()).calculateMinMax();
				window.refreshLabel(Labels.LINK);
			}
			else if(e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectLink(p[0], p[1]);
				Link link = ((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().getSelectedLink();
				try {
					link.setFreespeed(Double.parseDouble(JOptionPane.showInputDialog("New free speed", link.getFreespeed())));
				} catch(Exception e2) {
					
				}
				((SimpleSelectionNetworkPainter)getActiveLayer().getPainter()).calculateMinMax();
				window.refreshLabel(Labels.LINK);
			}
		}
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
			else if(window.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
				camera.zoomIn(p[0], p[1]);
			else if(window.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
				camera.zoomOut(p[0], p[1]);
		}
		repaint();
	}
	public String getLabelText(playground.sergioo.visualizer2D2012.LayersWindow.Labels label) {
		try {
			return (String) CarNetworkPainterManager.class.getMethod("refresh"+label.getText(), new Class[0]).invoke(((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager(), new Object[0]);
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
		case 's':
			((SimpleSelectionNetworkPainter)getPrincipalLayer().getPainter()).changeVisibleSelectedElements();
			break;
		case 'o':
			((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager().selectOppositeLink();
			window.refreshLabel(Labels.LINK);
			break;
		case 'v':
			viewAll();
			break;
		case '+':
			if(addressLocator.getNumResults()>0) {
				posLocation++;
				if(posLocation==addressLocator.getNumResults())
					posLocation = 0;
				Coord c = addressLocator.getLocation(posLocation);
				centerCamera(new double[]{c.getX(), c.getY()});
			}
			break;
		case '-':
			if(addressLocator.getNumResults()>0) {
				posLocation--;
				if(posLocation<0)
					posLocation = addressLocator.getNumResults()-1;
				Coord c = addressLocator.getLocation(posLocation);
				centerCamera(new double[]{c.getX(), c.getY()});
			}
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
