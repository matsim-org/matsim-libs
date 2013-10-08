package playground.sergioo.eventAnalysisTools2013.excessWaitingTime;

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
	private Map<Coord, Double[]> points = new HashMap<Coord, Double[]>();
	private Color[] colors;
	private double scale;
	
	//Methods
	public StationsPainter(Color[] colors, double scale) {
		super();
		this.colors = colors;
		this.scale = scale;
	}
	public void addPoint(Coord point, Double[] size) {
		points.put(point, size);
	}
	public void clearPoints() {
		points.clear();
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		for(Entry<Coord, Double[]> point:points.entrySet()) {
			double angle = Math.PI/2, diff=2*Math.PI/point.getValue().length;
			double sum = 0;
			for(int i=0; i<point.getValue().length; i++) {
				//paintCircularRegion(g2, layersPanel, point.getKey(), (int)Math.sqrt(point.getValue()[i]*scale), angle+Math.PI/6, angle+diff-Math.PI/6, colors[i]);
				paintAngledBar(g2, layersPanel, point.getKey(), 15, (int)Math.sqrt(point.getValue()[i]*scale), angle, angle+diff, colors[i], true);
				angle += diff;
				sum += point.getValue()[i]<0?0:point.getValue()[i]*scale;
			}
			paintCircumference(g2, layersPanel, point.getKey(), (int)(Math.sqrt(sum/point.getValue().length)), Color.BLACK);
		}
	}

}
