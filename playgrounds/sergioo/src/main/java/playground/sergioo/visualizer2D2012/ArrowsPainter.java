package playground.sergioo.visualizer2D2012;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.core.utils.collections.Tuple;

import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.Painter;

public class ArrowsPainter extends Painter {
	
	
	//Attributes
	private List<Tuple<double[], double[]>> arrows = new ArrayList<Tuple<double[], double[]>>();
	private List<Color> colors = new ArrayList<Color>();
	private int longArrow = 5;
	private double angle = Math.PI/6;
	//Methods
	public void addLine(double[] pointA, double[] pointB) {
		arrows.add(new Tuple<double[], double[]>(pointA, pointB));
	}
	public void addColor(Color color) {
		colors.add(color);
	}
	public void clearLines() {
		arrows.clear();
	}
	public void setLongArrow(int longArrow) {
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
			for(Tuple<double[], double[]> line:arrows)
				paintArrow(g2, layersPanel, line.getFirst(), line.getSecond(), angle, layersPanel.getWorldDistance(longArrow), stroke, colorsI.next());
		}
		else
			for(Tuple<double[], double[]> line:arrows)
				paintArrow(g2, layersPanel, line.getFirst(), line.getSecond(), angle, layersPanel.getWorldDistance(longArrow), stroke, Color.RED);
	}

}
