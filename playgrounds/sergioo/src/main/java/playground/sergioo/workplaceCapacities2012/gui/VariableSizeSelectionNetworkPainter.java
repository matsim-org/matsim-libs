package playground.sergioo.workplaceCapacities2012.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import others.sergioo.visUtils.JetColor;

import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;

public class VariableSizeSelectionNetworkPainter extends NetworkPainter {
	
	//Attributes
	private Color selectedLinkColor = Color.GREEN;
	private Color selectedNodeColor = Color.MAGENTA;
	private Stroke selectedStroke = new BasicStroke(2);
	private boolean withSelected = true;
	private Map<Id<Link>, Double> linkWeights = new HashMap<Id<Link>, Double>();
	private double minLinkWeight;
	private double maxLinkWeight;
	
	//Methods
	public VariableSizeSelectionNetworkPainter(Network network) {
		super(network);
	}
	public VariableSizeSelectionNetworkPainter(Network network, Color networkColor) {
		super(network, networkColor);
	}
	public VariableSizeSelectionNetworkPainter(Network network, Color networkColor, Stroke networkStroke) {
		super(network, networkColor, networkStroke);
	}
	public VariableSizeSelectionNetworkPainter(Network network, Color networkColor, Stroke networkStroke, Color selectedLinkColor, Color selectedNodeColor, Stroke selectedStroke) {
		super(network, networkColor, networkStroke);
		this.selectedLinkColor = selectedLinkColor;
		this.selectedNodeColor = selectedNodeColor;
		this.selectedStroke = selectedStroke;
	}
	public void setlinkWeights(Map<Id<Link>, Double> linkWeights) {
		networkPainterManager.selectLinkIds(linkWeights.keySet());
		this.linkWeights = linkWeights;
		minLinkWeight = Double.MAX_VALUE;
		maxLinkWeight = 0;
		for(Double value:linkWeights.values()) {
			if(value<minLinkWeight)
				minLinkWeight = value;
			if(value>maxLinkWeight)
				maxLinkWeight = value;
		}
		if(minLinkWeight==maxLinkWeight)
			maxLinkWeight++;
	}
	@Override
	/*public void paint(Graphics2D g2, LayersPanel layersPanel) {
		super.paint(g2, layersPanel);
		if(withSelected) {
			paintSelected(g2, layersPanel);
			for(Link link:networkPainterManager.getSelectedLinks()) {
				Double weight = linkWeights.get(link.getId());
				double min = 10;
				if(weight!=null&&weight>min)
					paintSimpleLink(g2, layersPanel, link, new BasicStroke(5), JetColor.getJetColor((float) ((weight-minLinkWeight)/(maxLinkWeight-minLinkWeight))));
			}
			for(Node node:networkPainterManager.getSelectedNodes())
				paintCircle(g2, layersPanel, node.getCoord(), 5, selectedNodeColor);
		}
		JetColor.paintScale(g2, 10, 10, 300, 30, new Font("Calibri", Font.PLAIN, 14), Color.BLACK, minLinkWeight, maxLinkWeight, 5);
	}*/
	/*public void paint(Graphics2D g2, LayersPanel layersPanel) {
		super.paint(g2, layersPanel);
		if(withSelected) {
			double min = 0;
			float maxStroke = 25;
			paintSelected(g2, layersPanel);
			for(Link link:networkPainterManager.getSelectedLinks()) {
				Double weight = linkWeights.get(link.getId());
				if(weight!=null&&weight>min) {
					float proportion = (float)((weight-minLinkWeight)/(maxLinkWeight-minLinkWeight));
					paintSimpleLink(g2, layersPanel, link, new BasicStroke(proportion*maxStroke), JetColor.getJetColor(proportion));
				}
			}
			for(Node node:networkPainterManager.getSelectedNodes())
				paintCircle(g2, layersPanel, node.getCoord(), 5, selectedNodeColor);
		}
		JetColor.paintScale(g2, 10, 10, 300, 30, new Font("Calibri", Font.PLAIN, 14), Color.BLACK, minLinkWeight, maxLinkWeight, 5);
	}*/
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		super.paint(g2, layersPanel);
		if(withSelected) {
			double min = 0;
			float maxSize = 50;
			paintSelected(g2, layersPanel);
			for(Link link:networkPainterManager.getSelectedLinks()) {
				Double weight = linkWeights.get(link.getId());
				if(weight!=null && weight>min) {
					float proportion = (float)((weight-minLinkWeight)/(maxLinkWeight-minLinkWeight));
					paintCircle(g2, layersPanel, link.getToNode().getCoord(), (int)(proportion*maxSize), JetColor.getJetColor(proportion, 127));
				}
			}
			for(Node node:networkPainterManager.getSelectedNodes())
				paintCircle(g2, layersPanel, node.getCoord(), 5, selectedNodeColor);
		}
		JetColor.paintScale(g2, 10, 10, 300, 30, new Font("Calibri", Font.PLAIN, 14), Color.BLACK, minLinkWeight, maxLinkWeight, 5);
	}
	private void paintSelected(Graphics2D g2, LayersPanel layersPanel) {
		Link link = networkPainterManager.getSelectedLink();
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
