package playground.sergioo.NetworksMatcher.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.InvocationTargetException;

import playground.sergioo.NetworksMatcher.gui.DoubleNetworkWindow.Label;
import playground.sergioo.NetworksMatcher.gui.DoubleNetworkWindow.Option;
import playground.sergioo.Visualizer2D.Layer;
import playground.sergioo.Visualizer2D.NetworkVisualizer.DoubleNetwork.DoubleNetworkPanel;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkManager;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;

public class DoubleNetworkMatchingPanel extends DoubleNetworkPanel implements MouseListener, MouseMotionListener, KeyListener {
	
	//Attributes
	private final DoubleNetworkWindow doubleNetworkWindow;
	private int iniX;
	private int iniY;
	private boolean withNetwork = true;
	
	//Methods
	public DoubleNetworkMatchingPanel(DoubleNetworkWindow doubleNetworkWindow, NetworkPainter networkPainterA, NetworkPainter networkPainterB) {
		super(networkPainterA, networkPainterB);
		this.doubleNetworkWindow = doubleNetworkWindow;
		/*TODO addLayer(new Layer(new MatchingsPainter(), false));*/
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
	}
	public String getLabelText(Label label) {
		try {
			return (String) NetworkManager.class.getMethod("refresh"+label.getText(), new Class[0]).invoke(((NetworkPainter)getActiveLayer().getPainter()).getNetworkManager(), new Object[0]);
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
			if(doubleNetworkWindow.getOption().equals(Option.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkManager().selectLink(getWorldX(e.getX()),getWorldY(e.getY()));
				doubleNetworkWindow.refreshLabel(Label.LINK);
			}
			else if(doubleNetworkWindow.getOption().equals(Option.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkManager().unselectLink();
				doubleNetworkWindow.refreshLabel(Label.LINK);
			}
			else if(doubleNetworkWindow.getOption().equals(Option.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkManager().selectNode(getWorldX(e.getX()),getWorldY(e.getY()));
				doubleNetworkWindow.refreshLabel(Label.NODE);
			}
			else if(doubleNetworkWindow.getOption().equals(Option.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkManager().unselectNode();
				doubleNetworkWindow.refreshLabel(Label.NODE);
			}
			else if(doubleNetworkWindow.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
				camera.zoomIn(getWorldX(e.getX()), getWorldY(e.getY()));
			else if(doubleNetworkWindow.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
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
	public void keyTyped(KeyEvent e) {
		switch(e.getKeyChar()) {
		case 'a':
			changeActiveLayer();
			break;
		case 'n':
			withNetwork  = !withNetwork;
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
