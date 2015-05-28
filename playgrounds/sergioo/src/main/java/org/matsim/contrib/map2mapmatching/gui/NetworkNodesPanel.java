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

package org.matsim.contrib.map2mapmatching.gui;

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
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.map2mapmatching.gui.DoubleNetworkMatchingWindow.Labels;
import org.matsim.contrib.map2mapmatching.gui.DoubleNetworkMatchingWindow.Options;
import org.matsim.contrib.map2mapmatching.gui.MatchingsPainter.MatchingOptions;
import org.matsim.contrib.map2mapmatching.gui.core.Layer;
import org.matsim.contrib.map2mapmatching.gui.core.LayersPanel;
import org.matsim.contrib.map2mapmatching.gui.core.LayersWindow;
import org.matsim.contrib.map2mapmatching.gui.core.LinesPainter;
import org.matsim.contrib.map2mapmatching.gui.core.PointsPainter;
import org.matsim.contrib.map2mapmatching.gui.core.network.painter.NetworkPainter;
import org.matsim.contrib.map2mapmatching.kernel.CrossingMatchingStep;
import org.matsim.contrib.map2mapmatching.kernel.core.NodesMatching;
import org.matsim.core.utils.collections.Tuple;

public class NetworkNodesPanel extends LayersPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private final DoubleNetworkMatchingWindow doubleNetworkWindow;
	private int iniX;
	private int iniY;
	
	//Methods
	public NetworkNodesPanel(DoubleNetworkMatchingWindow doubleNetworkWindow, NetworkPainter networkPainter) {
		super();
		this.doubleNetworkWindow = doubleNetworkWindow;
		addLayer(new Layer(networkPainter));
		addLayer(new Layer(null));
		addLayer(new Layer(null));
		this.setBackground(backgroundColor);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	public NetworkNodesPanel(Collection<NodesMatching> nodesMatchings, MatchingOptions matchingOption, DoubleNetworkMatchingWindow doubleNetworkWindow, NetworkPainter networkPainter, List<Color> colors) {
		super();
		this.doubleNetworkWindow = doubleNetworkWindow;
		addLayer(new Layer(networkPainter));
		addLayer(new Layer(new MatchingsPainter(nodesMatchings, matchingOption, colors), false));
		addLayer(new Layer(new LinesPainter(), false));
		PointsPainter pointsPainter = new PointsPainter();
		Tuple<Id<Node>, Id<Node>>[] tuples = null;
		tuples = matchingOption.equals(MatchingsPainter.MatchingOptions.A)?CrossingMatchingStep.NEAR_NODES_LOW:CrossingMatchingStep.NEAR_NODES_HIGH;
		for(Tuple<Id<Node>, Id<Node>> ids:tuples) {
			Node node = networkPainter.getNetwork().getNodes().get(ids.getFirst());
			if(node!=null)
				pointsPainter.addPoint(node.getCoord());
		}
		addLayer(new Layer(pointsPainter));
		this.setBackground(backgroundColor);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	public void setMatchings(Collection<NodesMatching> nodesMatchings, MatchingOptions matchingOption, List<Color> colors) {
		getLayer(1).setPainter(new MatchingsPainter(nodesMatchings, matchingOption, colors));
	}
	public void setNetwork(Network network) {
		((NetworkTwoNodesPainter)getLayer(0).getPainter()).setNetwork(network);
	}
	public Set<Node> getSelectedNodes() {
		return ((NetworkNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).getSelectedNodesAndClear();
	}
	public void selectNodes(Set<Node> nodes) {
		((NetworkNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).selectNodes(nodes);
	}
	public void selectLink(String id) {
		Link link = ((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectLink(id);
		if(link!=null)
			doubleNetworkWindow.centerCamera(new double[]{link.getCoord().getX(), link.getCoord().getY()});
	}
	public void selectNode(String id) {
		Node node = ((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectNode(id);
		if(node!=null)
			doubleNetworkWindow.centerCamera(new double[]{node.getCoord().getX(), node.getCoord().getY()});
	}
	public Collection<? extends Link> getLinks() {
		return ((NetworkNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).getLinks();
	}
	public void setLinksLayer(Set<Link> wrongLinks) {
		((LinesPainter)getLayer(2).getPainter()).clearLines();
		for(Link link: wrongLinks) {
			Coord coord = link.getFromNode().getCoord();
			double[] pointF = new double[]{coord.getX(), coord.getY()};
			coord = link.getToNode().getCoord();
			double[] pointT = new double[]{coord.getX(), coord.getY()};
			((LinesPainter)getLayer(2).getPainter()).addLine(pointF, pointT);
		}
	}
	public void clearNodesSelection() {
		((NetworkNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).clearNodesSelection();
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
	public String getLabelText(LayersWindow.Labels label) {
		try {
			return (String) NetworkNodesPainterManager.class.getMethod("refresh"+label.getText(), new Class[0]).invoke(((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager(), new Object[0]);
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
		doubleNetworkWindow.setActivePanel(this);
		double[] p = getWorld(e.getX(), e.getY());
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(p);
		else {
			if(doubleNetworkWindow.getOption().equals(Options.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectLink(p[0], p[1]);
				doubleNetworkWindow.refreshLabel(Labels.LINK);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().unselectLink();
				doubleNetworkWindow.refreshLabel(Labels.LINK);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODES) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).selectNearestNode(p[0], p[1]);
				doubleNetworkWindow.refreshLabel(Labels.NODES);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODES) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).unselectNearestNode(p[0], p[1]);
				doubleNetworkWindow.refreshLabel(Labels.NODES);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON1) {
				camera.zoomIn(p[0], p[1]);
				doubleNetworkWindow.cameraChange(camera);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON3) {
				camera.zoomOut(p[0], p[1]);
				doubleNetworkWindow.cameraChange(camera);
			}
		}
		repaint();
	}
	@Override
	public void mousePressed(MouseEvent e) {
		this.requestFocus();
		doubleNetworkWindow.setActivePanel(this);
		doubleNetworkWindow.refreshLabel(Labels.ACTIVE);
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
		doubleNetworkWindow.cameraChange(camera);
		repaint();
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		double[] p = getWorld(e.getX(), e.getY());
		doubleNetworkWindow.setCoords(p[0], p[1]);
	}
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		this.requestFocus();
		doubleNetworkWindow.setActivePanel(this);
		if(e.getWheelRotation()<0)
			camera.zoomIn();
		else if(e.getWheelRotation()>0)
			camera.zoomOut();
		doubleNetworkWindow.cameraChange(camera);
		repaint();
	}
	@Override
	public void keyTyped(KeyEvent e) {
		switch(e.getKeyChar()) {
		case 'o':
			((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager().selectOppositeLink();
			doubleNetworkWindow.refreshLabel(Labels.LINK);
			break;
		case 'v':
			viewAll();
			doubleNetworkWindow.cameraChange(camera);
			break;
		case 'm':
			doubleNetworkWindow.setNetworksSeparated();
			break;
		case 'f':
			doubleNetworkWindow.finalNetworks();
			break;
		case 'l':
			getLayer(2).changeVisible();
			break;
		case 'p':
			getLayer(3).changeVisible();
			break;
		case 'n':
			getLayer(1).changeVisible();
			break;
		}
		repaint();
	}
	@Override
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_UP:
			doubleNetworkWindow.nextNetwork();
			break;
		case KeyEvent.VK_DOWN:
			doubleNetworkWindow.previousNetwork();
			break;
		case KeyEvent.VK_SPACE:
			doubleNetworkWindow.selectMatch();
			break;
		case KeyEvent.VK_ENTER:
			if(doubleNetworkWindow.getSelectedNodesMatching()==null)
				doubleNetworkWindow.match();
			else
				doubleNetworkWindow.modifyMatch();
			break;
		case KeyEvent.VK_BACK_SPACE:
			doubleNetworkWindow.deleteMatch();
			break;
		case KeyEvent.VK_DELETE:
			doubleNetworkWindow.clearSelection();
			break;
		}
		doubleNetworkWindow.setVisible(true);
		doubleNetworkWindow.repaint();
	}
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	
}
