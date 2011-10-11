package playground.sergioo.CountsFileGenerator;

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
	
	//Methods
	public void addPoint(Coord point) {
		points.add(point);
		selectedPoint = point;
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		for(Coord point:points)
			paintCircle(g2, layersPanel, point, 2, Color.BLACK);
		paintCircle(g2, layersPanel, selectedPoint, 4, Color.RED);
	}
	public void selectPoint(Coord point) {
		selectedPoint = point;
	}

}
