package org.matsim.contrib.map2mapmatching.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.map2mapmatching.gui.core.LayersPanel;
import org.matsim.contrib.map2mapmatching.gui.core.network.painter.NetworkPainter;

public class NetworkNodesPainter extends NetworkPainter {
	
	//Attributes
	private Color selectedNodesColor = Color.MAGENTA;
	private Color selectedNodeColor = Color.CYAN;
	private Color selectedLinkColor = Color.GREEN;
	private Stroke selectedStroke = new BasicStroke(2);
	private boolean withSelected = true;
	
	//Methods
	public NetworkNodesPainter(Network network) {
		super(network, new NetworkNodesPainterManager(network));
	}
	public NetworkNodesPainter(Network network, Color networkColor, Stroke networkStroke) {
		super(network, new NetworkNodesPainterManager(network), networkColor, networkStroke);
	}
	public NetworkNodesPainter(Network network, Color networkColor) {
		super(network, new NetworkNodesPainterManager(network), networkColor);
	}
	public NetworkNodesPainter(Network network, Color networkColor, Stroke networkStroke, Color selectedNodesColor) {
		super(network, new NetworkNodesPainterManager(network), networkColor, networkStroke);
		this.selectedNodesColor = selectedNodesColor;
	}
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		super.paint(g2, layersPanel);
		for(Node node:((NetworkNodesPainterManager)networkPainterManager).getSelectedNodes())
			if(node!=null)
				paintCircle(g2, layersPanel, node.getCoord(), 6, selectedNodesColor);
		if(withSelected)
			paintSelected(g2, layersPanel);
	}
	private void paintSelected(Graphics2D g2, LayersPanel layersPanel) {
		Link link=networkPainterManager.getSelectedLink();
		if(link!=null)
			paintLink(g2, layersPanel, link, selectedStroke, 3, selectedLinkColor);
		Node node=networkPainterManager.getSelectedNode();
		if(node!=null)
			paintX(g2, layersPanel, node.getCoord(), 4, selectedNodeColor);
	}
	public void changeVisibleSelectedElements() {
		withSelected = !withSelected;
	}
	
}
