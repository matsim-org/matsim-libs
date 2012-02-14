package playground.sergioo.CapacitiesChanger.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;

public class SimpleSelectionNetworkPainter extends NetworkPainter {
	
	//Attributes
	private Color selectedLinkColor = Color.CYAN;
	private Color selectedNodeColor = Color.MAGENTA;
	private Stroke selectedStroke = new BasicStroke(2);
	private boolean withSelected = true;
	private double minCapacity;
	private double maxCapacity;
	
	//Methods
	public SimpleSelectionNetworkPainter(Network network) {
		super(network);
		calculateMinMax();
	}
	public SimpleSelectionNetworkPainter(Network network, Color networkColor) {
		super(network, networkColor);
		calculateMinMax();
	}
	public SimpleSelectionNetworkPainter(Network network, Color networkColor, Stroke networkStroke) {
		super(network, networkColor, networkStroke);
		calculateMinMax();
	}
	public SimpleSelectionNetworkPainter(Network network, Color networkColor, Stroke networkStroke, Color selectedLinkColor, Color selectedNodeColor, Stroke selectedStroke) {
		super(network, networkColor, networkStroke);
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
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		try {
			for(Link link:networkPainterManager.getNetworkLinks(layersPanel.getCamera()))
				paintSimpleLink(g2, layersPanel, link, new BasicStroke(2.5f), getCapacityColor(link.getCapacity()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(withSelected)
			paintSelected(g2, layersPanel);
	}
	private Color getCapacityColor(double capacity) {
		float prop = (float) ((capacity-minCapacity)/(maxCapacity-minCapacity));
		return new Color(1-prop, 0, prop);
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
