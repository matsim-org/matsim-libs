package playground.sergioo.Visualizer2D;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.Painter;

public class PointsPainter extends Painter {
	
	
	//Attributes
	private Collection<Coord> points = new ArrayList<Coord>();
	private Coord selectedPoint;
	private Color color = Color.BLACK;
	
	//Methods
	public PointsPainter() {
		super();
	}
	public PointsPainter(Color color) {
		super();
		this.color = color;
	}
	public Collection<Coord> getPoints() {
		return points;
	}
	public void addPoint(Coord point) {
		points.add(point);
	}
	public void clearPoints() {
		points.clear();
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		for(Coord point:points)
			paintX(g2, layersPanel, point, 3, color);
		if(selectedPoint!=null)
			paintCircle(g2, layersPanel, selectedPoint, 4, Color.RED);
	}
	public void selectPoint(Coord point) {
		if(points.contains(point))
			selectedPoint = point;
	}

}
