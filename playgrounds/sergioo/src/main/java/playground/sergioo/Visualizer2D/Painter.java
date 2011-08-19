package playground.sergioo.Visualizer2D;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;


public abstract class Painter {

	//Methods
	public abstract void paint(Graphics2D g2, LayersPanel layersPanel);
	protected void paintLine(Graphics2D g2, LayersPanel layersPanel, Tuple<Coord,Coord> coords, Stroke stroke, Color color) {
		g2.setStroke(stroke);
		g2.setColor(color);
		g2.drawLine(layersPanel.getScreenX(coords.getFirst().getX()),
				layersPanel.getScreenY(coords.getFirst().getY()),
				layersPanel.getScreenX(coords.getSecond().getX()),
				layersPanel.getScreenY(coords.getSecond().getY()));
	}
	protected void paintCircle(Graphics2D g2, LayersPanel layersPanel, Coord coord, double pointSize, Color color) {
		g2.setColor(color);
		Shape circle = new Ellipse2D.Double(layersPanel.getScreenX(coord.getX())-pointSize,layersPanel.getScreenY(coord.getY())-pointSize,pointSize*2,pointSize*2);
		g2.fill(circle);
	}
	
}
