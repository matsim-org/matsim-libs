package playground.sergioo.NetworksMatcher.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import playground.sergioo.NetworksMatcher.gui.DoubleNetworkMatchingWindow.Labels;
import playground.sergioo.NetworksMatcher.gui.DoubleNetworkMatchingWindow.Options;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;
import playground.sergioo.Visualizer2D.Layer;
import playground.sergioo.Visualizer2D.NetworkVisualizer.DoubleNetwork.DoubleNetworkPanel;

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
	public DoubleNetworkMatchingPanel(DoubleNetworkMatchingWindow doubleNetworkWindow, NetworkNodesPainter networkPainterA, NetworkNodesPainter networkPainterB) {
		super(networkPainterA, networkPainterB);
		this.doubleNetworkWindow = doubleNetworkWindow;
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
	}
	public DoubleNetworkMatchingPanel(Collection<NodesMatching> nodesMatchings, DoubleNetworkMatchingWindow doubleNetworkWindow, NetworkNodesPainter networkPainterA, NetworkNodesPainter networkPainterB) {
		super(networkPainterA, networkPainterB);
		this.doubleNetworkWindow = doubleNetworkWindow;
		addLayer(new Layer(new MatchingsPainter(nodesMatchings), false));
		matchingsAdded = true;
		addMouseListener(this);
		addMouseMotionListener(this);
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
	public String getLabelText(Labels label) {
		try {
			return (String) NetworkNodesPainterManager.class.getMethod("refresh"+label.getText(), new Class[0]).invoke(((NetworkNodesPainter)getActiveLayer().getPainter()).getNetworkManager(), new Object[0]);
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
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(getWorldX(e.getX()), getWorldY(e.getY()));
		else {
			if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODES) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkNodesPainterManager)((NetworkNodesPainter)getActiveLayer().getPainter()).getNetworkManager()).selectNodeFromCollection(getWorldX(e.getX()),getWorldY(e.getY()));
				doubleNetworkWindow.refreshLabel(Labels.NODES);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODES) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkNodesPainterManager)((NetworkNodesPainter)getActiveLayer().getPainter()).getNetworkManager()).unselectNodeFromCollection(getWorldX(e.getX()),getWorldY(e.getY()));
				doubleNetworkWindow.refreshLabel(Labels.NODES);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
				camera.zoomIn(getWorldX(e.getX()), getWorldY(e.getY()));
			else if(doubleNetworkWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
				camera.zoomOut(getWorldX(e.getX()), getWorldY(e.getY()));
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
		camera.move(getWorldX(e.getX()),getWorldX(iniX),getWorldY(e.getY()),getWorldY(iniY));
		iniX = e.getX();
		iniY = e.getY();
		repaint();
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		doubleNetworkWindow.setCoords(getWorldX(e.getX()),getWorldY(e.getY()));
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
			((NetworkNodesPainter)getPrincipalLayer().getPainter()).changeVisibleSelectedElements();
			break;
		case 'o':
			((NetworkNodesPainter)getPrincipalLayer().getPainter()).getNetworkManager().selectOppositeLink();
			doubleNetworkWindow.refreshLabel(Labels.LINK);
			break;
		case 'n':
			getActiveLayer().changeVisible();
			break;
		case 'v':
			viewAll();
			doubleNetworkWindow.cameraChange(camera);
			break;
		case 'm':
			doubleNetworkWindow.setNetworksSeparated();
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
