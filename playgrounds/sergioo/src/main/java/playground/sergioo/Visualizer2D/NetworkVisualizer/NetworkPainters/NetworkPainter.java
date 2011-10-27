package playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.Painter;

public class NetworkPainter extends Painter {
	
	//Constants
	private static final double ANGLE_ARROW = Math.PI/6;
	private static final double LONG_ARROW = 10;
	
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
				paintSimpleLink(g2, layersPanel, link, networkStroke, networkColor);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	protected void paintSimpleLink(Graphics2D g2, LayersPanel layersPanel, Link link, Stroke stroke, Color color) {
		Coord from = link.getFromNode().getCoord();
		Coord to = link.getToNode().getCoord();
		paintLine(g2, layersPanel, new Tuple<Coord, Coord>(from, to), stroke, color);
	}
	protected void paintLink(Graphics2D g2, LayersPanel layersPanel, Link link, Stroke stroke, double pointSize, Color color) {
		Coord from = link.getFromNode().getCoord();
		Coord to = link.getToNode().getCoord();
		double angle = Math.atan2(to.getY()-from.getY(), to.getX()-from.getX());;
		paintLine(g2, layersPanel, new Tuple<Coord, Coord>(from, to), stroke, color);
		paintLine(g2, layersPanel, new Tuple<Coord, Coord>(to, new CoordImpl(to.getX()-LONG_ARROW*pointSize*Math.sin(Math.PI/2-angle-ANGLE_ARROW), to.getY()-LONG_ARROW*pointSize*Math.cos(Math.PI/2-angle-ANGLE_ARROW))), stroke, color);
		paintLine(g2, layersPanel, new Tuple<Coord, Coord>(to, new CoordImpl(to.getX()-LONG_ARROW*pointSize*Math.sin(Math.PI/2-angle+ANGLE_ARROW), to.getY()-LONG_ARROW*pointSize*Math.cos(Math.PI/2-angle+ANGLE_ARROW))), stroke, color);
	}
	
}
