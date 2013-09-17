package playground.sergioo.eventAnalysisTools2013.backwards;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;

import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.Painter;

public class StationsPainter extends Painter {
	
	//Attributes
	private Map<Coord, Double> points = new HashMap<Coord, Double>();
	private Color color;
	private double scale;
	
	//Methods
	public StationsPainter(Color color, double scale) {
		super();
		this.color = color;
		this.scale = scale;
	}
	public void addPoint(Coord point) {
		points.put(point, 0.0);
	}
	public void increaseSize(Coord point) {
		points.put(point, points.get(point)+1);
	}
	public void clearPoints() {
		points.clear();
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		for(Entry<Coord, Double> point:points.entrySet()) {
			paintCross(g2, layersPanel, point.getKey(), 3, color);
			paintCircle(g2, layersPanel, point.getKey(), (int)(Math.sqrt(point.getValue())*scale), color);
		}
	}

}
