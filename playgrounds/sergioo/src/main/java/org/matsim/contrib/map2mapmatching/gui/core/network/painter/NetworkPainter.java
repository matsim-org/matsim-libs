package org.matsim.contrib.map2mapmatching.gui.core.network.painter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.map2mapmatching.gui.core.LayersPanel;
import org.matsim.contrib.map2mapmatching.gui.core.Painter;

public class NetworkPainter extends Painter {
	
	//Constants
	private static final double ANGLE_ARROW = Math.PI/6;
	private static final double LONG_ARROW = 10;
	
	//Attributes
	protected NetworkPainterManager networkPainterManager;
	protected double time;
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
	public void setTime(double time) {
		this.time = time;
	}
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		try {
			Collection<Link> rails = new ArrayList<Link>();
			for(Link link:networkPainterManager.getNetworkLinks(layersPanel.getCamera()))
				if(link.getAllowedModes().contains("car"))
					paintSimpleLink(g2, layersPanel, link, networkStroke, networkColor);
				else if(link.getAllowedModes().contains("bus"))
					paintSimpleLink(g2, layersPanel, link, networkStroke, new Color(0.8f,0.8f,1.0f));
				else
					rails.add(link);
			for(Link link:rails)
				paintSimpleLink(g2, layersPanel, link, new BasicStroke(((BasicStroke)networkStroke).getLineWidth()*2), Color.DARK_GRAY);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	protected void paintSimpleLink(Graphics2D g2, LayersPanel layersPanel, Link link, Stroke stroke, Color color) {
		double[] from = new double[]{link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()};
		double[] to = new double[]{link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY()};
		paintLine(g2, layersPanel, from, to, stroke, color);
	}
	protected void paintLink(Graphics2D g2, LayersPanel layersPanel, Link link, Stroke stroke, double pointSize, Color color) {
		double[] from = new double[]{link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()};
		double[] to = new double[]{link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY()};
		paintArrow(g2, layersPanel, from, to, ANGLE_ARROW, LONG_ARROW*pointSize, stroke, color);
	}
	
}
