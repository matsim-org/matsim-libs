package playground.sergioo.visualizer2D2012;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

public class LinesPainter extends Painter {

	//Attributes
	private Collection<Tuple<double[], double[]>> lines = new ArrayList<Tuple<double[], double[]>>();
	private Stroke stroke= new BasicStroke(2);
	private Color color = Color.PINK;
	
	//Methods
	public void addLine(Coord pointA, Coord pointB) {
		lines.add(new Tuple<double[], double[]>(new double[]{pointA.getX(), pointA.getY()}, new double[]{pointB.getX(), pointB.getY()}));
	}
	public void addLine(double[] pointA, double[] pointB) {
		lines.add(new Tuple<double[], double[]>(pointA, pointB));
	}
	public void clearLines() {
		lines.clear();
	}
	public void setStroke(Stroke stroke) {
		this.stroke = stroke;
	}
	public void setColor(Color color) {
		this.color = color;
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		for(Tuple<double[], double[]> line:lines)
			paintLine(g2, layersPanel, line.getFirst(), line.getSecond(), stroke, color );
	}

}
