package playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;

import playground.sergioo.Visualizer2D.Camera;
import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.Painter;

public abstract class NetworkPainter extends Painter {
	
	//Attributes
	protected final NetworkByCamera networkByCamera;
	protected final NetworkManager networkManager;
	
	//Methods
	public NetworkPainter(Network network) {
		networkByCamera =  new NetworkByCamera(network);
		networkManager = new NetworkManager(network);
	}
	public NetworkManager getNetworkManager() {
		return networkManager;
	}
	public Camera getCamera() {
		return networkByCamera.getCamera();
	}
	public void setCamera(Camera camera) {
		networkByCamera.setCamera(camera);
	}
	protected void paintLink(Graphics2D g2, LayersPanel layersPanel, Link link, Stroke stroke, double pointSize, Color color) {
		paintLine(g2, layersPanel, new Tuple<Coord, Coord>(link.getFromNode().getCoord(), link.getToNode().getCoord()), stroke, color);
		paintCircle(g2, layersPanel, link.getToNode().getCoord(), pointSize, color);
	}
	
}
