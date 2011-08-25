package playground.sergioo.NetworksMatcher.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;

public class NetworkNodesPainter extends NetworkPainter {
	
	//Attributes
	private Color selectedNodesColor = Color.MAGENTA;
	private Color selectedLinkColor = Color.GREEN;
	private Color selectedNodeColor = Color.MAGENTA;
	private Stroke selectedStroke = new BasicStroke(2);
	private boolean withSelected = true;
	
	//Methods
	public NetworkNodesPainter(Network network) {
		super(network, new NetworkNodesManager(network));
	}
	public NetworkNodesPainter(Network network, Color networkColor, Stroke networkStroke) {
		super(network, new NetworkNodesManager(network), networkColor, networkStroke);
	}
	public NetworkNodesPainter(Network network, Color networkColor, Color selectedNodesColor) {
		super(network, new NetworkNodesManager(network), networkColor);
		this.selectedNodesColor = selectedNodesColor;
	}
	public NetworkNodesPainter(Network network, Color networkColor, Stroke networkStroke, Color selectedNodesColor) {
		super(network, new NetworkNodesManager(network), networkColor, networkStroke);
		this.selectedNodesColor = selectedNodesColor;
	}
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		super.paint(g2, layersPanel);
		for(Node node:((NetworkNodesManager)networkManager).getSelectedNodes())
			if(node!=null)
				paintCircle(g2, layersPanel, node.getCoord(), 5, selectedNodesColor);
		if(withSelected)
			paintSelected(g2, layersPanel);
	}
	private void paintSelected(Graphics2D g2, LayersPanel layersPanel) {
		Link link=networkManager.getSelectedLink();
		if(link!=null)
			paintLink(g2, layersPanel, link, selectedStroke, 3, selectedLinkColor);
		Node node = networkManager.getSelectedNode();
		if(node!=null)
			paintCircle(g2, layersPanel, node.getCoord(), 5, selectedNodeColor);
	}
	public void changeVisibleSelectedElements() {
		withSelected = !withSelected;
	}
	
}
