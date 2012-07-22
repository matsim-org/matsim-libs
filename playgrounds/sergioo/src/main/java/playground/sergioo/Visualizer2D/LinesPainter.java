package playground.sergioo.Visualizer2D;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.utils.collections.Tuple;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.Painter;

public class LinesPainter extends Painter {
	
	
	//Attributes
	private Collection<Tuple<double[], double[]>> lines = new ArrayList<Tuple<double[], double[]>>();
	
	//Methods
	public void addLine(double[] pointA, double[] pointB) {
		lines.add(new Tuple<double[], double[]>(pointA, pointB));
	}
	public void clearLines() {
		lines.clear();
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		Stroke stroke = new BasicStroke(2);
		for(Tuple<double[], double[]> line:lines)
			paintLine(g2, layersPanel, line.getFirst(), line.getSecond(), stroke, Color.PINK);
	}

}
