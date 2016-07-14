package playground.sergioo.capacitiesChanger2012.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import others.sergioo.visUtils.JetColor;

import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;

public class SimpleSelectionNetworkPainter extends NetworkPainter {
	
	//Constants
	private static final float MAX_WIDTH = 7;
	
	//Attributes
	private Color selectedLinkColor = Color.CYAN;
	private Color selectedNodeColor = Color.MAGENTA;
	private Stroke selectedStroke = new BasicStroke(2);
	private boolean withSelected = true;
	private double minCapacity;
	private double maxCapacity;
	private double minFreeSpeed;
	private double maxFreeSpeed;
	
	//Methods
	public SimpleSelectionNetworkPainter(Network network) {
		super(network);
		networkPainterManager = new CarNetworkPainterManager(network);
		calculateMinMax();
	}
	public SimpleSelectionNetworkPainter(Network network, Color networkColor) {
		super(network, networkColor);
		networkPainterManager = new CarNetworkPainterManager(network);
		calculateMinMax();
	}
	public SimpleSelectionNetworkPainter(Network network, Color networkColor, Stroke networkStroke) {
		super(network, networkColor, networkStroke);
		networkPainterManager = new CarNetworkPainterManager(network);
		calculateMinMax();
	}
	public SimpleSelectionNetworkPainter(Network network, Color networkColor, Stroke networkStroke, Color selectedLinkColor, Color selectedNodeColor, Stroke selectedStroke) {
		super(network, networkColor, networkStroke);
		networkPainterManager = new CarNetworkPainterManager(network);
		this.selectedLinkColor = selectedLinkColor;
		this.selectedNodeColor = selectedNodeColor;
		this.selectedStroke = selectedStroke;
		calculateMinMax();
	}
	public void calculateMinMax() {
		minCapacity = Double.MAX_VALUE;
		maxCapacity = 0;
		for(Link link:networkPainterManager.getNetwork().getLinks().values()) {
			if(link.getCapacity()>maxCapacity)
				maxCapacity = link.getCapacity();
			if(link.getCapacity()<minCapacity)
				minCapacity = link.getCapacity();
		}
		minFreeSpeed = Double.MAX_VALUE;
		maxFreeSpeed = 0;
		for(Link link:networkPainterManager.getNetwork().getLinks().values()) {
			if(link.getFreespeed()>maxFreeSpeed)
				maxFreeSpeed = link.getFreespeed();
			if(link.getFreespeed()<minFreeSpeed)
				minFreeSpeed = link.getFreespeed();
		}
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		try {
			for(Link link:networkPainterManager.getNetworkLinks(layersPanel.getCamera()))
				paintSimpleLink(g2, layersPanel, link, new BasicStroke(getCapacityWidth(link.getCapacity())), getFreeSpeedColor(link.getFreespeed()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(withSelected)
			paintSelected(g2, layersPanel);
	}
	private Color getFreeSpeedColor(double freeSpeed) {
		float prop = (float) ((freeSpeed-3*minFreeSpeed)/(maxFreeSpeed-3*minFreeSpeed));
		if(prop<0)
			prop=0;
		return JetColor.getJetColor(prop);
	}
	private float getCapacityWidth(double capacity) {
		float prop = (float) ((capacity-minCapacity)/(maxCapacity-minCapacity));
		return prop*MAX_WIDTH;
	}
	private void paintSelected(Graphics2D g2, LayersPanel layersPanel) {
		Link link=networkPainterManager.getSelectedLink();
		if(link!=null)
			paintLink(g2, layersPanel, link, selectedStroke, 3, selectedLinkColor);
		Node node = networkPainterManager.getSelectedNode();
		if(node!=null)
			paintCircle(g2, layersPanel, node.getCoord(), 5, selectedNodeColor);
	}
	public void changeVisibleSelectedElements() {
		withSelected = !withSelected;
	}
	
}
