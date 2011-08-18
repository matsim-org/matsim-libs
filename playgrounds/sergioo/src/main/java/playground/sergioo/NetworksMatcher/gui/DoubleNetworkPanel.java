package playground.sergioo.NetworksMatcher.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import playground.sergioo.Visualizer2D.Layer;
import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;

public class DoubleNetworkPanel extends LayersPanel implements MouseListener, MouseMotionListener, KeyListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public DoubleNetworkPanel(DoubleNetworkWindow doubleNetworkWindow, NetworkPainter networkPainterA, NetworkPainter networkPainterB) {
		super();
		layers.add(new Layer(networkPainterA));
		layers.add(new Layer(networkPainterB));
	}
	@Override
	public void keyTyped(KeyEvent e) {
		
	}
	@Override
	public void keyPressed(KeyEvent e) {
		
	}
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		
	}
	@Override
	public void mousePressed(MouseEvent e) {
		
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
	
}
