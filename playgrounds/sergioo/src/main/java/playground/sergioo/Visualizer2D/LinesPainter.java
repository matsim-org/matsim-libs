package playground.sergioo.Visualizer2D;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.Painter;

public class LinesPainter extends Painter {
	
	
	//Attributes
	private Collection<Tuple<Coord,Coord>> lines = new ArrayList<Tuple<Coord,Coord>>();
	
	//Methods
	public void addLine(Coord pointA, Coord pointB) {
		lines.add(new Tuple<Coord, Coord>(pointA, pointB));
	}
	public void clearLines() {
		lines.clear();
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		Stroke stroke = new BasicStroke(2);
		for(Tuple<Coord, Coord> line:lines)
			paintLine(g2, layersPanel, line, stroke, Color.PINK);
	}

}
