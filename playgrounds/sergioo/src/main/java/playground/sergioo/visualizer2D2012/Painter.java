package playground.sergioo.visualizer2D2012;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;

import org.matsim.api.core.v01.Coord;


public abstract class Painter {

	//Methods
	public abstract void paint(Graphics2D g2, LayersPanel layersPanel);
	protected void paintLine(Graphics2D g2, LayersPanel layersPanel, double[] pointA, double[] pointB, Stroke stroke, Color color) {
		g2.setStroke(stroke);
		g2.setColor(color);
		int[] screenPointA = layersPanel.getScreenXY(pointA);
		int[] screenPointB = layersPanel.getScreenXY(pointB);
		g2.drawLine(screenPointA[0], screenPointA[1],screenPointB[0],screenPointB[1]);
	}
	protected void paintCircle(Graphics2D g2, LayersPanel layersPanel, Coord coord, double pointSize, Color color) {
		g2.setColor(color);
		int[] screenPoint = layersPanel.getScreenXY(new double[]{coord.getX(), coord.getY()});
		Shape circle = new Ellipse2D.Double(screenPoint[0]-pointSize,screenPoint[1]-pointSize,pointSize*2,pointSize*2);
		g2.fill(circle);
		g2.setColor(Color.DARK_GRAY);
		g2.draw(circle);
	}
	protected void paintCross(Graphics2D g2, LayersPanel layersPanel, Coord coord, double pointSize, Color color) {
		Stroke stroke= g2.getStroke();
		g2.setColor(color);
		g2.setStroke(new BasicStroke((float) (pointSize)));
		int[] screenPoint = layersPanel.getScreenXY(new double[]{coord.getX(), coord.getY()});
		g2.drawLine(screenPoint[0]-(int)pointSize, screenPoint[1], screenPoint[0]+(int)pointSize, screenPoint[1]);
		g2.drawLine(screenPoint[0], screenPoint[1]-(int)pointSize, screenPoint[0], screenPoint[1]+(int)pointSize);
		g2.setStroke(new BasicStroke((float) (pointSize)));
		g2.setStroke(stroke);
	}
	protected void paintX(Graphics2D g2, LayersPanel layersPanel, Coord coord, double pointSize, Color color) {
		g2.setColor(color);
		g2.setStroke(new BasicStroke((float) (pointSize)));
		int[] screenPoint = layersPanel.getScreenXY(new double[]{coord.getX(), coord.getY()});
		g2.drawLine(screenPoint[0]-(int)pointSize, screenPoint[1]+(int)pointSize, screenPoint[0]+(int)pointSize, screenPoint[1]-(int)pointSize);
		g2.drawLine(screenPoint[0]-(int)pointSize, screenPoint[1]-(int)pointSize, screenPoint[0]+(int)pointSize, screenPoint[1]+(int)pointSize);
	}
	protected void paintArrow(Graphics2D g2, LayersPanel layersPanel, double[] pointA, double[] pointB, double angleArrow, double longArrow, Stroke stroke, Color color) {
		double angle = Math.atan2(pointB[1]-pointA[1], pointB[0]-pointA[0]);
		paintLine(g2, layersPanel, pointA, pointB, stroke, color);
		paintLine(g2, layersPanel, pointB, new double[]{pointB[0]-longArrow*Math.sin(Math.PI/2-angle-angleArrow), pointB[1]-longArrow*Math.cos(Math.PI/2-angle-angleArrow)}, stroke, color);
		paintLine(g2, layersPanel, pointB, new double[]{pointB[0]-longArrow*Math.sin(Math.PI/2-angle+angleArrow), pointB[1]-longArrow*Math.cos(Math.PI/2-angle+angleArrow)}, stroke, color);
	}
	protected void paintPolygon(Graphics2D g2, LayersPanel layersPanel, double[][] points, Color color) {
		g2.setColor(color);
		Polygon polygon = new Polygon();
		for(double[] point:points) {
			int[] screenPoint = layersPanel.getScreenXY(point);
			polygon.addPoint(screenPoint[0], screenPoint[1]);
		}
		g2.fill(polygon);
	}
	protected void paintVertical3DScale(Graphics2D g2, LayersPanel layersPanel,	double startHeight, double endHeight, double startValue, double endValue, int divisions, double baseSize, Font font, NumberFormat format, Stroke stroke, Color color) {
		double[] base = new double[]{layersPanel.getCamera().getCenter()[0], layersPanel.getCamera().getCenter()[1], startHeight};
		double[] top = new double[]{layersPanel.getCamera().getCenter()[0], layersPanel.getCamera().getCenter()[1], endHeight};
		paintLine(g2, layersPanel, base, top, stroke, color);
		paintLine(g2, layersPanel, new double[]{base[0]+baseSize, base[1], 0}, new double[]{base[0]-baseSize, base[1], 0}, stroke, color);
		paintLine(g2, layersPanel, new double[]{base[0], base[1]+baseSize, 0}, new double[]{base[0], base[1]-baseSize, 0}, stroke, color);
		double deltaHeight = (endHeight-startHeight)/divisions;
		double deltaValue = (endValue-startValue)/divisions;
		g2.setFont(font);
		for(double i=1; i<divisions+1; i++) {
			int[] screen = layersPanel.getScreenXY(new double[]{base[0], base[1], i*deltaHeight});	
			g2.drawLine(screen[0]-4, screen[1], screen[0]+4, screen[1]);
			g2.drawRect(screen[0]+4, screen[1]-font.getSize()-2, (font.getSize()/2)*format.format(i*deltaValue).length()+6, font.getSize()+6);
			g2.setColor(new Color(255-color.getRed(), 255-color.getGreen(), 255-color.getBlue()));
			g2.fillRect(screen[0]+4, screen[1]-font.getSize()-2, (font.getSize()/2)*format.format(i*deltaValue).length()+6, font.getSize()+6);
			g2.setColor(color);
			g2.drawString(format.format(i*deltaValue)+"", screen[0]+7, screen[1]);
		}
	}
}
