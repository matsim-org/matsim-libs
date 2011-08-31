package playground.sergioo.NetworksMatcher.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Collection;

import org.matsim.api.core.v01.network.Node;

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
	private final MatchingOptions option;
	
	//Methods
	public MatchingsPainter(Collection<NodesMatching> nodesMatchings) {
		super();
		this.nodesMatchings = nodesMatchings;
		option = MatchingOptions.BOTH;
	}
	public MatchingsPainter(Collection<NodesMatching> nodesMatchings, MatchingOptions option) {
		super();
		this.nodesMatchings = nodesMatchings;
		this.option = option;
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		for(NodesMatching nodesMatching:nodesMatchings) {
			Color colorA = new Color((float)Math.random()*0.5f+0.5f,(float)Math.random()*0.5f+0.5f,0*((float)Math.random()*0.5f+0.5f));
			Color colorB = new Color(colorA.getRed()/2,colorA.getGreen()/2,colorA.getBlue()/2);
			if(option.equals(MatchingOptions.BOTH) || option.equals(MatchingOptions.A)) {
				for(Node node:nodesMatching.getComposedNodeA().getNodes())
					paintCircle(g2, layersPanel, node.getCoord(), 3, colorA);
			}
			if(option.equals(MatchingOptions.BOTH) || option.equals(MatchingOptions.B)) {
				for(Node node:nodesMatching.getComposedNodeB().getNodes())
					paintCircle(g2, layersPanel, node.getCoord(), 3, colorB);
			}
		}
	}

}
