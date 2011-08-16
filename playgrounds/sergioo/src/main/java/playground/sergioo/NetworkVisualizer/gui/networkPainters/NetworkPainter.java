package playground.sergioo.NetworkVisualizer.gui.networkPainters;

import java.awt.Graphics2D;

import playground.sergioo.NetworkVisualizer.gui.Camera;

public interface NetworkPainter {
	
	//Methods
	public NetworkByCamera getNetworkByCamera();
	public void paintNetwork(Graphics2D g2, Camera camera) throws Exception;
	
}
