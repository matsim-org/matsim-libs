package playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.Visualizer2D.LayersPanel;

public class SimpleNetworkPainter extends NetworkPainter {
	
	//Attributes
	private Color networkColor = Color.LIGHT_GRAY;
	private Stroke networkStroke = new BasicStroke(0.5f);
	private Color linkSelectedColor = Color.GREEN;
	private Color nodeSelectedColor = Color.MAGENTA;
	private Stroke selectedStroke = new BasicStroke(2);
	private boolean withSelected = true;
	
	//Methods
	public SimpleNetworkPainter(Network network) {
		super(network);
	}
	public SimpleNetworkPainter(Network network, Color networkColor) {
		super(network);
		this.networkColor = networkColor;
	}
	public SimpleNetworkPainter(Network network, Color networkColor, Stroke networkStroke) {
		super(network);
		this.networkColor = networkColor;
		this.networkStroke = networkStroke;
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		networkByCamera.setCamera(layersPanel.getCamera());
		try {
			for(Link link:networkByCamera.getNetworkLinks())
				paintLink(g2, layersPanel, link, networkStroke, 0.5, networkColor);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(withSelected)
			paintSelected(g2, layersPanel);
	}
	private void paintSelected(Graphics2D g2, LayersPanel layersPanel) {
		Link link=networkManager.getSelectedLink();
		if(link!=null)
			paintLink(g2, layersPanel, link, selectedStroke, 3, linkSelectedColor);
		Node node = networkManager.getSelectedNode();
		if(node!=null)
			paintCircle(g2, layersPanel, node.getCoord(), 5, nodeSelectedColor);
	}
	public void changeSelected() {
		withSelected = !withSelected;
	}
	
}
