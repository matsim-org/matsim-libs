package playground.sergioo.Visualizer2D;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.Painter;

public class ArrowsPainter extends Painter {
	
	
	//Attributes
	private List<Tuple<Coord,Coord>> arrows = new ArrayList<Tuple<Coord,Coord>>();
	private List<Color> colors = new ArrayList<Color>();
	private double longArrow = 30;
	private double angle = Math.PI/6;
	//Methods
	public void addLine(Coord pointA, Coord pointB) {
		arrows.add(new Tuple<Coord, Coord>(pointA, pointB));
	}
	public void addColor(Color color) {
		colors.add(color);
	}
	public void clearLines() {
		arrows.clear();
	}
	public void setLongArrow(double longArrow) {
		this.longArrow = longArrow;
	}
	public void setAngle(double angle) {
		this.angle = angle;
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		Stroke stroke = new BasicStroke(0.5f);
		if(colors.size() == arrows.size()) {
			Iterator<Color> colorsI = colors.iterator();
			for(Tuple<Coord, Coord> line:arrows)
				paintArrow(g2, layersPanel, line, angle, longArrow, stroke, colorsI.next());
		}
		else
			for(Tuple<Coord, Coord> line:arrows)
				paintArrow(g2, layersPanel, line, angle, longArrow, stroke, Color.RED);
	}

}
