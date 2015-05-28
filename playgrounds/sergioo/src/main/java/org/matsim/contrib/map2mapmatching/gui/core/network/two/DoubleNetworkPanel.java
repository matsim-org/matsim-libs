package org.matsim.contrib.map2mapmatching.gui.core.network.two;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.map2mapmatching.gui.core.Layer;
import org.matsim.contrib.map2mapmatching.gui.core.LayersPanel;
import org.matsim.contrib.map2mapmatching.gui.core.network.painter.NetworkPainter;


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
		Collection<double[]> coords = new ArrayList<double[]>();
		for(Layer layer:getAllLayers())
			for(Link link:((NetworkPainter)layer.getPainter()).getNetwork().getLinks().values()) {
				if(link!=null) {
					coords.add(new double[]{link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()});
					coords.add(new double[]{link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY()});
				}
			}
		super.calculateBoundaries(coords);
	}
	public void setNetworkPainters(NetworkPainter networkPainterA, NetworkPainter networkPainterB) {
		removeFirstLayer();
		removeFirstLayer();
		//TODO
	}
	
}
