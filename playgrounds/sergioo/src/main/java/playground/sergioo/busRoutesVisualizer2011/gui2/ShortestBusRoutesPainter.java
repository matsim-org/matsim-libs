package playground.sergioo.busRoutesVisualizer2011.gui2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.LinesPainter;
import playground.sergioo.visualizer2D2012.PointsPainter;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;

public class ShortestBusRoutesPainter extends NetworkPainter {

	private PointsPainter[] stops;
	private LinesPainter[] links;

	public ShortestBusRoutesPainter(Network network, Set<Coord>[] stopCoords, Set<Link>[] links) {
		super(network);
		this.stops = new PointsPainter[stopCoords.length];
		for(int i=0; i<this.stops.length; i++) {
			this.stops[i] = new PointsPainter(new Color((int) (Math.random()*16777216)));
			this.stops[i].setPointSize(4);
			for(Coord coord:stopCoords[stops.length-i-1])
				this.stops[i].addPoint(coord);
		}
		this.links = new LinesPainter[links.length];
		for(int i=0; i<this.links.length; i++) {
			this.links[i] = new LinesPainter();
			this.links[i].setColor(new Color((int) (Math.random()*16777216)));
			this.links[i].setStroke(new BasicStroke(2.5f));
			for(Link link:links[links.length-i-1])
				this.links[i].addLine(link.getFromNode().getCoord(), link.getToNode().getCoord());
		}
	}
	
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel)  {
		super.paint(g2, layersPanel);
		for(LinesPainter painter:links)
			painter.paint(g2, layersPanel);
		for(PointsPainter painter:stops)
			painter.paint(g2, layersPanel);
	}

}
