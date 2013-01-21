package playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.Painter;

public class DynamicNetworkPainter extends Painter {
	
	//Constants
	private static final double ANGLE_ARROW = Math.PI/6;
	private static final double LONG_ARROW = 10;
	
	//Attributes
	protected DynamicNetworkPainterManager dynamicNetworkPainterManager;
	private Color networkColor = Color.LIGHT_GRAY;
	private Stroke networkStroke = new BasicStroke(0.5f);
	
	//Methods
	public DynamicNetworkPainter(Network network) {
		dynamicNetworkPainterManager = new DynamicNetworkPainterManager(network);
	}
	public DynamicNetworkPainter(Network network, Color networkColor, Stroke networkStroke) {
		dynamicNetworkPainterManager = new DynamicNetworkPainterManager(network);
		this.networkColor = networkColor;
		this.networkStroke = networkStroke;
	}
	public DynamicNetworkPainter(Network network, double timeStep, double totalTime) {
		dynamicNetworkPainterManager = new DynamicNetworkPainterManager(network, timeStep, totalTime);
	}
	public DynamicNetworkPainter(Network network, Color networkColor, Stroke networkStroke, double timeStep, double totalTime) {
		dynamicNetworkPainterManager = new DynamicNetworkPainterManager(network, timeStep, totalTime);
		this.networkColor = networkColor;
		this.networkStroke = networkStroke;
	}
	public DynamicNetworkPainter(Network network, DynamicNetworkPainterManager networkManager) {
		this.dynamicNetworkPainterManager = networkManager;
	}
	public DynamicNetworkPainter(Network network, DynamicNetworkPainterManager networkManager, Color networkColor) {
		this.dynamicNetworkPainterManager = networkManager;
		this.networkColor = networkColor;
	}
	public DynamicNetworkPainter(Network network, DynamicNetworkPainterManager networkManager, Color networkColor, Stroke networkStroke) {
		this.dynamicNetworkPainterManager = networkManager;
		this.networkColor = networkColor;
		this.networkStroke = networkStroke;
	}
	public DynamicNetworkPainterManager getNetworkPainterManager() {
		return dynamicNetworkPainterManager;
	}
	public Network getNetwork() {
		return dynamicNetworkPainterManager.getNetwork();
	}
	public void setNetwork(Network network) {
		dynamicNetworkPainterManager.setNetwork(network);
	}
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		try {
			Collection<Link> rails = new ArrayList<Link>();
			for(Link link:dynamicNetworkPainterManager.getNetworkLinks(layersPanel.getCamera()))
				if(link.getAllowedModes().contains("car"))
					paintSimpleLink(g2, layersPanel, link, networkStroke, networkColor);
				else if(link.getAllowedModes().contains("bus"))
					paintSimpleLink(g2, layersPanel, link, networkStroke, new Color(0.8f,0.8f,1.0f));
				else
					rails.add(link);
			for(Link link:rails)
				paintSimpleLink(g2, layersPanel, link, new BasicStroke(1), new Color(0,0,0));
			paintTime(g2, layersPanel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void paintTime(Graphics2D g2, LayersPanel layersPanel) {
		g2.setFont(new Font("Arial", Font.PLAIN, 20));
		g2.setColor(networkColor);
		g2.drawString(new SimpleDateFormat("HH:mm:ss").format(new Date((long) (dynamicNetworkPainterManager.getTime()-27000)*1000)), layersPanel.getSize().width-100, layersPanel.getSize().height-20);
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
	public void setTime(double time) {
		dynamicNetworkPainterManager.setTime(time);
	}
	public double getTime() {
		return dynamicNetworkPainterManager.getTime();
	}

}
