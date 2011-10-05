package playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.Painter;

public class NetworkPainter extends Painter {
	
	//Attributes
	protected final NetworkPainterManager networkPainterManager;
	private Color networkColor = Color.LIGHT_GRAY;
	private Stroke networkStroke = new BasicStroke(0.5f);
	
	//Methods
	public NetworkPainter(Network network) {
		networkPainterManager = new NetworkPainterManager(network);
	}
	public NetworkPainter(Network network, Color networkColor) {
		networkPainterManager = new NetworkPainterManager(network);
		this.networkColor = networkColor;
	}
	public NetworkPainter(Network network, Color networkColor, Stroke networkStroke) {
		networkPainterManager = new NetworkPainterManager(network);
		this.networkColor = networkColor;
		this.networkStroke = networkStroke;
	}
	public NetworkPainter(Network network, NetworkPainterManager networkManager) {
		this.networkPainterManager = networkManager;
	}
	public NetworkPainter(Network network, NetworkPainterManager networkManager, Color networkColor) {
		this.networkPainterManager = networkManager;
		this.networkColor = networkColor;
	}
	public NetworkPainter(Network network, NetworkPainterManager networkManager, Color networkColor, Stroke networkStroke) {
		this.networkPainterManager = networkManager;
		this.networkColor = networkColor;
		this.networkStroke = networkStroke;
	}
	public NetworkPainterManager getNetworkPainterManager() {
		return networkPainterManager;
	}
	public Network getNetwork() {
		return networkPainterManager.getNetwork();
	}
	public void setNetwork(Network network) {
		networkPainterManager.setNetwork(network);
	}
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		try {
			for(Link link:networkPainterManager.getNetworkLinks(layersPanel.getCamera()))
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
