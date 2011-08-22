package playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters;

import java.awt.BasicStroke;
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

public class NetworkPainter extends Painter {
	
	//Attributes
	protected final NetworkByCamera networkByCamera;
	protected final NetworkManager networkManager;
	private Color networkColor = Color.LIGHT_GRAY;
	private Stroke networkStroke = new BasicStroke(0.5f);
	
	//Methods
	public NetworkPainter(Network network) {
		networkByCamera =  new NetworkByCamera(network);
		networkManager = new NetworkManager(network);
	}
	public NetworkPainter(Network network, Color networkColor) {
		networkByCamera =  new NetworkByCamera(network);
		networkManager = new NetworkManager(network);
		this.networkColor = networkColor;
	}
	public NetworkPainter(Network network, Color networkColor, Stroke networkStroke) {
		networkByCamera =  new NetworkByCamera(network);
		networkManager = new NetworkManager(network);
		this.networkColor = networkColor;
		this.networkStroke = networkStroke;
	}
	public NetworkPainter(Network network, NetworkManager networkManager) {
		networkByCamera =  new NetworkByCamera(network);
		this.networkManager = networkManager;
	}
	public NetworkPainter(Network network, NetworkManager networkManager, Color networkColor) {
		networkByCamera =  new NetworkByCamera(network);
		this.networkManager = networkManager;
		this.networkColor = networkColor;
	}
	public NetworkPainter(Network network, NetworkManager networkManager, Color networkColor, Stroke networkStroke) {
		networkByCamera =  new NetworkByCamera(network);
		this.networkManager = networkManager;
		this.networkColor = networkColor;
		this.networkStroke = networkStroke;
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
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		networkByCamera.setCamera(layersPanel.getCamera());
		try {
			for(Link link:networkByCamera.getNetworkLinks())
				paintLink(g2, layersPanel, link, networkStroke, 0.5, networkColor);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	protected void paintLink(Graphics2D g2, LayersPanel layersPanel, Link link, Stroke stroke, double pointSize, Color color) {
		paintLine(g2, layersPanel, new Tuple<Coord, Coord>(link.getFromNode().getCoord(), link.getToNode().getCoord()), stroke, color);
		paintCircle(g2, layersPanel, link.getToNode().getCoord(), pointSize, color);
	}
	
}
