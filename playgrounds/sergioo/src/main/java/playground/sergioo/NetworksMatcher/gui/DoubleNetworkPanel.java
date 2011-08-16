package playground.sergioo.NetworksMatcher.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;

import playground.sergioo.NetworkVisualizer.gui.NetworkManager;
import playground.sergioo.NetworkVisualizer.gui.NetworkPanel;
import playground.sergioo.NetworkVisualizer.gui.networkPainters.NetworkPainter;

public class DoubleNetworkPanel extends NetworkPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final NetworkManager networkManagerB;
	private final NetworkPainter networkPainterB;
	
	public DoubleNetworkPanel(DoubleNetworkWindow doubleNetworkWindow, NetworkManager networkManagerA, NetworkPainter networkPainterA, NetworkManager networkManagerB, NetworkPainter networkPainterB) {
		super(doubleNetworkWindow, networkManagerA, networkPainterA);
		this.networkManagerB = networkManagerB;
		this.networkPainterB = networkPainterB;
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2=(Graphics2D)g;
		try {
			networkPainterB.paintNetwork(g2, this.getCamera());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
