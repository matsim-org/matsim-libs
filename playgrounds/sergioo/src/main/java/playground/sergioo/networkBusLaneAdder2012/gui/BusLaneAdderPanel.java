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

package playground.sergioo.networkBusLaneAdder2012.gui;

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
import java.util.List;

import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.SAXException;

import others.sergioo.addressLocator2011.AddressLocator;
import others.sergioo.addressLocator2011.BadAddressException;
import playground.sergioo.networkBusLaneAdder2012.gui.BusLaneAdderWindow.Labels;
import playground.sergioo.networkBusLaneAdder2012.gui.BusLaneAdderWindow.Options;
import playground.sergioo.visualizer2D2012.ImagePainter;
import playground.sergioo.visualizer2D2012.Layer;
import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.LinesPainter;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;

public class BusLaneAdderPanel extends LayersPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final double BUS_SPEED = 16/3.6;
	
	//Attributes
	private final BusLaneAdderWindow busLaneAdderWindow;
	private int iniX;
	private int iniY;
	private Dijkstra dijkstra;
	private int posLocation = 0;
	private AddressLocator addressLocator;
	
	//Methods
	public BusLaneAdderPanel(BusLaneAdderWindow busLaneAdderWindow, NetworkPainter networkPainter, File imageFile, double[] upLeft, double[] downRight, CoordinateTransformation coordinateTransformation) throws IOException {
		super();
		addressLocator = new AddressLocator(coordinateTransformation);
		this.busLaneAdderWindow = busLaneAdderWindow;
		ImagePainter imagePainter = new ImagePainter(imageFile, this);
		imagePainter.setImageCoordinates(upLeft, downRight);
		addLayer(new Layer(imagePainter, false));
		addLayer(new Layer(networkPainter), true);
		addLayer(new Layer(new LinesPainter(), false));
		this.setBackground(backgroundColor);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
		TravelDisutility travelMinCost = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				if(link.getAllowedModes().contains("bus"))
					return link.getLength()/BUS_SPEED;
				else
					return Double.MAX_VALUE;
			}
		};
		TravelTime timeFunction = new TravelTime() {	
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				if(link.getAllowedModes().contains("bus"))
					return link.getLength()/BUS_SPEED;
				else
					return Double.MAX_VALUE;
			}
		};
		PreProcessDijkstra preProcessData = new PreProcessDijkstra();
		preProcessData.run(busLaneAdderWindow.getNetwork());
		dijkstra = new Dijkstra(busLaneAdderWindow.getNetwork(), travelMinCost, timeFunction, preProcessData);
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
	public void clearSelection() {
		((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).clearNodesSelection();
		((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).clearLinksSelection();
	}
	public void selectLinks() {
		((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).selectLinks(dijkstra);
	}
	public List<Link> getLinks() {
		return ((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).getSelectedLinks();
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
				Coord c=addressLocator.getLocation(posLocation);
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
	public String getLabelText(playground.sergioo.visualizer2D2012.LayersWindow.Labels label) {
		try {
			return (String) NetworkTwoNodesPainterManager.class.getMethod("refresh"+label.getText(), new Class[0]).invoke(((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager(), new Object[0]);
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
	public void mouseClicked(MouseEvent e) {
		double[] p = getWorld(e.getX(), e.getY());
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(p);
		else {
			if(busLaneAdderWindow.getOption().equals(Options.SELECT_NODES) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).selectNearestNode(p[0], p[1]);
				busLaneAdderWindow.refreshLabel(Labels.NODES);
			}
			else if(busLaneAdderWindow.getOption().equals(Options.SELECT_NODES) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).unselectNearestNode(p[0], p[1]);
				busLaneAdderWindow.refreshLabel(Labels.NODES);
			}
			else if(busLaneAdderWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON1) {
				camera.zoomIn(p[0], p[1]);
			}
			else if(busLaneAdderWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON3) {
				camera.zoomOut(p[0], p[1]);
			}
		}
		repaint();
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
		busLaneAdderWindow.setCoords(p[0], p[1]);
	}
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		this.requestFocus();
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
		switch(e.getKeyCode()) {
		case KeyEvent.VK_DELETE:
			((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).clearNodesSelection();
			break;
		}
		busLaneAdderWindow.setVisible(true);
		busLaneAdderWindow.repaint();
	}
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	
}
