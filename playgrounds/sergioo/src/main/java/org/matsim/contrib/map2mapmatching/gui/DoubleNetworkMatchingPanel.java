package org.matsim.contrib.map2mapmatching.gui;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.map2mapmatching.gui.DoubleNetworkMatchingWindow.Labels;
import org.matsim.contrib.map2mapmatching.gui.DoubleNetworkMatchingWindow.Options;
import org.matsim.contrib.map2mapmatching.gui.MatchingsPainter.MatchingOptions;
import org.matsim.contrib.map2mapmatching.gui.core.Layer;
import org.matsim.contrib.map2mapmatching.gui.core.network.two.DoubleNetworkPanel;
import org.matsim.contrib.map2mapmatching.kernel.core.NodesMatching;

public class DoubleNetworkMatchingPanel extends DoubleNetworkPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private final DoubleNetworkMatchingWindow doubleNetworkWindow;
	private int iniX;
	private int iniY;
	private boolean matchingsAdded = false;
	
	//Methods
	public DoubleNetworkMatchingPanel(DoubleNetworkMatchingWindow doubleNetworkWindow, NetworkTwoNodesPainter networkPainterA, NetworkTwoNodesPainter networkPainterB) {
		super(networkPainterA, networkPainterB);
		this.doubleNetworkWindow = doubleNetworkWindow;
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
	}
	public DoubleNetworkMatchingPanel(Collection<NodesMatching> nodesMatchings, DoubleNetworkMatchingWindow doubleNetworkWindow, NetworkTwoNodesPainter networkPainterA, NetworkTwoNodesPainter networkPainterB, List<Color> colors) {
		super(networkPainterA, networkPainterB);
		this.doubleNetworkWindow = doubleNetworkWindow;
		addLayer(new Layer(new MatchingsPainter(nodesMatchings, MatchingOptions.BOTH, colors), false));
		matchingsAdded = true;
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
	}
	public void setMatchings(Collection<NodesMatching> nodesMatchings) {
		if(!matchingsAdded) {
			addLayer(new Layer(new MatchingsPainter(nodesMatchings), false));
			matchingsAdded = true;
		}
		else {
			removeLastLayer();
			addLayer(new Layer(new MatchingsPainter(nodesMatchings), false));
		}
	}
	public void setNetworks(Network networkA, Network networkB) {
		((NetworkTwoNodesPainter)getLayer(0).getPainter()).setNetwork(networkA);
		((NetworkTwoNodesPainter)getLayer(1).getPainter()).setNetwork(networkB);
	}
	public String getLabelText(Labels label) {
		try {
			return (String) NetworkNodesPainterManager.class.getMethod("refresh"+label.getText(), new Class[0]).invoke(((NetworkTwoNodesPainter)getActiveLayer().getPainter()).getNetworkPainterManager(), new Object[0]);
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
			if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODES) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkNodesPainterManager)((NetworkTwoNodesPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).selectNearestNode(p[0], p[1]);
				doubleNetworkWindow.refreshLabel(Labels.NODES);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODES) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkNodesPainterManager)((NetworkTwoNodesPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).unselectNearestNode(p[0], p[1]);
				doubleNetworkWindow.refreshLabel(Labels.NODES);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
				camera.zoomIn(p[0], p[1]);
			else if(doubleNetworkWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
				camera.zoomOut(p[0], p[1]);
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
		doubleNetworkWindow.setCoords(p[0], p[1]);
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
		case 's':
			((NetworkTwoNodesPainter)getPrincipalLayer().getPainter()).changeVisibleSelectedElements();
			break;
		case 'o':
			((NetworkTwoNodesPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager().selectOppositeLink();
			doubleNetworkWindow.refreshLabel(Labels.LINK);
			break;
		case 'n':
			getActiveLayer().changeVisible();
			break;
		case 'v':
			viewAll();
			break;
		case 'm':
			doubleNetworkWindow.setNetworksSeparated();
			break;
		case 'f':
			doubleNetworkWindow.finalNetworks();
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
		}
		repaint();
	}
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	
}
