package playground.sergioo.NetworksMatcher.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.network.Node;

import playground.sergioo.NetworksMatcher.kernel.core.ComposedNode;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;
import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.Painter;

public class MatchingsPainter extends Painter {
	
	//Enumerations
	public enum MatchingOptions {
		A,
		B,
		BOTH;
	}
	
	//Attributes
	private Collection<NodesMatching> nodesMatchings;
	private List<Color> colors;
	private MatchingOptions option;
	
	//Methods
	public MatchingsPainter(Collection<NodesMatching> nodesMatchings) {
		super();
		this.nodesMatchings = nodesMatchings;
		option = MatchingOptions.BOTH;
		colors = generateRandomColors(nodesMatchings.size());
	}
	public MatchingsPainter(Collection<NodesMatching> nodesMatchings, MatchingOptions option, List<Color> colors) {
		super();
		this.nodesMatchings = nodesMatchings;
		this.option = option;
		this.colors = colors;
	}
	public static List<Color> generateRandomColors(int size) {
		List<Color> colors = new ArrayList<Color>();
		for(int i=0; i<size; i++) {
			float r = 0,g = 0,b = 0;
			while(r+b+g<1) {
				r = (float) Math.random();
				b = (float) Math.random();
				g = (float) Math.random();
			}
			colors.add(new Color(r, g, b));
		}
		return colors;
	}
	public void setOption(MatchingOptions option) {
		this.option = option;
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		Iterator<Color> colorIterator = colors.iterator();
		for(NodesMatching nodesMatching:nodesMatchings) {
			Color color = colorIterator.next();
			if(option.equals(MatchingOptions.BOTH) || option.equals(MatchingOptions.A)) {
				for(Node node:nodesMatching.getComposedNodeA().getNodes())
					paintCircle(g2, layersPanel, node.getCoord(), 4, color);
			}
			paintCircle(g2, layersPanel, nodesMatching.getComposedNodeA().getCoord(), layersPanel.getScreenX(ComposedNode.radius), new Color(color.getRed(), color.getGreen(), color.getBlue(), 128));
			if(option.equals(MatchingOptions.BOTH) || option.equals(MatchingOptions.B)) {
				for(Node node:nodesMatching.getComposedNodeB().getNodes())
					paintCircle(g2, layersPanel, node.getCoord(), 4, new Color(color.getRed()*7/8, color.getGreen()*7/8, color.getBlue()*7/8));
			}
			paintCircle(g2, layersPanel, nodesMatching.getComposedNodeB().getCoord(), layersPanel.getScreenX(ComposedNode.radius), new Color(color.getRed(), color.getGreen(), color.getBlue(), 128));
		}
	}

}
