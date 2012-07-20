package playground.sergioo.Visualizer2D;

import java.awt.BasicStroke;
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
	protected void paintLine(Graphics2D g2, LayersPanel layersPanel, Tuple<double[], double[]> coords, Stroke stroke, Color color) {
		g2.setStroke(stroke);
		g2.setColor(color);
		int[] screenPointA = layersPanel.getScreenXY(coords.getFirst());
		int[] screenPointB = layersPanel.getScreenXY(coords.getSecond());
		g2.drawLine(screenPointA[0], screenPointA[1],screenPointB[0],screenPointB[1]);
	}
	protected void paintCircle(Graphics2D g2, LayersPanel layersPanel, Coord coord, double pointSize, Color color) {
		g2.setColor(color);
		int[] screenPoint = layersPanel.getScreenXY(new double[]{coord.getX(), coord.getY()});
		Shape circle = new Ellipse2D.Double(screenPoint[0]-pointSize,screenPoint[1]-pointSize,pointSize*2,pointSize*2);
		g2.fill(circle);
	}
	protected void paintCross(Graphics2D g2, LayersPanel layersPanel, Coord coord, double pointSize, Color color) {
		g2.setColor(color);
		g2.setStroke(new BasicStroke((float) (pointSize)));
		int[] screenPoint = layersPanel.getScreenXY(new double[]{coord.getX(), coord.getY()});
		g2.drawLine(screenPoint[0]-(int)pointSize, screenPoint[1], screenPoint[0]+(int)pointSize, screenPoint[1]);
		g2.drawLine(screenPoint[0], screenPoint[1]-(int)pointSize, screenPoint[0], screenPoint[1]+(int)pointSize);
	}
	protected void paintX(Graphics2D g2, LayersPanel layersPanel, Coord coord, double pointSize, Color color) {
		g2.setColor(color);
		g2.setStroke(new BasicStroke((float) (pointSize)));
		int[] screenPoint = layersPanel.getScreenXY(new double[]{coord.getX(), coord.getY()});
		g2.drawLine(screenPoint[0]-(int)pointSize, screenPoint[1]+(int)pointSize, screenPoint[0]+(int)pointSize, screenPoint[1]-(int)pointSize);
		g2.drawLine(screenPoint[0]-(int)pointSize, screenPoint[1]-(int)pointSize, screenPoint[0]+(int)pointSize, screenPoint[1]+(int)pointSize);
	}
	protected void paintArrow(Graphics2D g2, LayersPanel layersPanel, Tuple<double[], double[]> points, double angleArrow, double longArrow, Stroke stroke, Color color) {
		double angle = Math.atan2(points.getSecond()[1]-points.getFirst()[1], points.getSecond()[0]-points.getFirst()[0]);
		paintLine(g2, layersPanel, points, stroke, color);
		paintLine(g2, layersPanel, new Tuple<double[], double[]>(points.getSecond(), new double[]{points.getSecond()[0]-longArrow*Math.sin(Math.PI/2-angle-angleArrow), points.getSecond()[1]-longArrow*Math.cos(Math.PI/2-angle-angleArrow)}), stroke, color);
		paintLine(g2, layersPanel, new Tuple<double[], double[]>(points.getSecond(), new double[]{points.getSecond()[0]-longArrow*Math.sin(Math.PI/2-angle+angleArrow), points.getSecond()[1]-longArrow*Math.cos(Math.PI/2-angle+angleArrow)}), stroke, color);
	}
	
}
