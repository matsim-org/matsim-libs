package playground.sergioo.NetworkVisualizer.gui;

import java.awt.Graphics2D;

public interface NetworkPainter {
	
	//Methods
	public NetworkByCamera getNetworkByCamera();
	public void paintNetwork(Graphics2D g2, Camera camera) throws Exception;
	
}
