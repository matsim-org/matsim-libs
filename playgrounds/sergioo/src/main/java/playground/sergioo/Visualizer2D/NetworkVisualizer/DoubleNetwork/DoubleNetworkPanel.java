package playground.sergioo.Visualizer2D.NetworkVisualizer.DoubleNetwork;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import playground.sergioo.Visualizer2D.Layer;
import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;

public class DoubleNetworkPanel extends LayersPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Methods
	public DoubleNetworkPanel(NetworkPainter networkPainterA, NetworkPainter networkPainterB) {
		super();
		addLayer(new Layer(networkPainterA));
		addLayer(new Layer(networkPainterB));
		this.setBackground(backgroundColor);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		setFocusable(true);
	}
	private void calculateBoundaries() {
		Collection<Coord> coords = new ArrayList<Coord>();
		for(Layer layer:getAllLayers())
			for(Link link:((NetworkPainter)layer.getPainter()).getNetworkManager().getNetworkLinks()) {
				if(link!=null) {
					coords.add(link.getFromNode().getCoord());
					coords.add(link.getToNode().getCoord());
				}
			}
		super.calculateBoundaries(coords);
	}
	
}
