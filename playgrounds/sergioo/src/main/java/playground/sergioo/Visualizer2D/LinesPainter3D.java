package playground.sergioo.Visualizer2D;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math.geometry.Vector3D;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.Painter;

public class LinesPainter3D extends Painter {

	//Classes
	private class Line {
		double[] pointA;
		double[] pointB;
		Color color;
		Stroke stroke;
		public Line(double[] pointA, double[] pointB, Color color, Stroke stroke) {
			super();
			this.pointA = pointA;
			this.pointB = pointB;
			this.color = color;
			this.stroke = stroke;
		}
	}

	//Attributes
	private SortedMap<Double, Line> linesMap = new TreeMap<Double, Line>();
	private Camera3DOrtho1 camera;
	
	//Methods
	public void addLine(double[] pointA, double[] pointB, Color color, Stroke stroke) {
		Vector3D center = new Vector3D(pointA[0], pointA[1], pointA[2]); 
		double distance = camera==null?linesMap.size():camera.getDistanceToCamera(center);
		while(linesMap.get(-distance)!=null)
			distance-=0.001;
		linesMap.put(-distance, new Line(pointA, pointB, color, stroke));
	}
	public void clearLines() {
		linesMap.clear();
	}
	public void setCamera(Camera3DOrtho1 camera) {
		boolean refresh = false;
		if(this.camera==null)
			refresh = true;
		this.camera = camera;
		if(refresh)
			refreshLinesOrder();
	}
	public void refreshLinesOrder() {
		Collection<Line> lines = new ArrayList<Line>();
		for(Line line:linesMap.values())
			lines.add(line);
		linesMap.clear();
		for(Line line:lines)
			addLine(line.pointA, line.pointB, line.color, line.stroke);
	}
	//@Override
	public void paint2(Graphics2D g2, LayersPanel layersPanel) {
		double side = 10;
		for(Entry<Double, Line> line:linesMap.entrySet())
			if(line.getKey()<0) {
				double x = line.getValue().pointA[0];
				double y = line.getValue().pointA[1];
				double z = line.getValue().pointB[2];
				double[][] sideA = new double[][]{{x+side, y-side, 0}, {x+side, y+side, 0}, {x+side, y+side, z}, {x+side, y-side, z}};
				paintPolygon(g2, layersPanel, sideA, line.getValue().color.darker().darker());
				double[][] sideB = new double[][]{{x+side, y+side, 0}, {x-side, y+side, 0}, {x-side, y+side, z}, {x+side, y+side, z}};
				paintPolygon(g2, layersPanel, sideB, line.getValue().color);
				double[][] sideC = new double[][]{{x-side, y+side, 0}, {x-side, y-side, 0}, {x-side, y-side, z}, {x-side, y+side, z}};
				paintPolygon(g2, layersPanel, sideC, line.getValue().color);
				double[][] sideD = new double[][]{{x-side, y-side, 0}, {x+side, y-side, 0}, {x+side, y-side, z}, {x-side, y-side, z}};
				paintPolygon(g2, layersPanel, sideD, line.getValue().color.darker().darker());
				double[][] up = new double[][]{{x-side, y-side, z}, {x+side, y-side, z}, {x+side, y+side, z}, {x-side, y+side, z}};
				paintPolygon(g2, layersPanel, up, line.getValue().color.darker());
			}
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		double side = 10;
		for(Entry<Double, Line> line:linesMap.entrySet())
			if(line.getKey()<0) {
				double x = line.getValue().pointA[0];
				double y = line.getValue().pointA[1];
				double z = line.getValue().pointB[2];
				double[][] sideA = new double[][]{{x+side, y-side, line.getValue().pointA[2]}, {x+side, y+side, line.getValue().pointA[2]}, {x+side, y+side, line.getValue().pointB[2]}, {x+side, y-side, line.getValue().pointB[2]}};
				paintPolygon(g2, layersPanel, sideA, line.getValue().color.darker().darker());
				double[][] sideB = new double[][]{{x+side, y+side, line.getValue().pointA[2]}, {x-side, y+side, line.getValue().pointA[2]}, {x-side, y+side, line.getValue().pointB[2]}, {x+side, y+side, line.getValue().pointB[2]}};
				paintPolygon(g2, layersPanel, sideB, line.getValue().color);
				double[][] sideC = new double[][]{{x-side, y+side, line.getValue().pointA[2]}, {x-side, y-side, line.getValue().pointA[2]}, {x-side, y-side, line.getValue().pointB[2]}, {x-side, y+side, line.getValue().pointB[2]}};
				paintPolygon(g2, layersPanel, sideC, line.getValue().color);
				double[][] sideD = new double[][]{{x-side, y-side, line.getValue().pointA[2]}, {x+side, y-side, line.getValue().pointA[2]}, {x+side, y-side, line.getValue().pointB[2]}, {x-side, y-side, line.getValue().pointB[2]}};
				paintPolygon(g2, layersPanel, sideD, line.getValue().color.darker().darker());
				double[][] up = new double[][]{{x-side, y-side, z}, {x+side, y-side, z}, {x+side, y+side, z}, {x-side, y+side, z}};
				paintPolygon(g2, layersPanel, up, line.getValue().color.darker());
			}
	}

}
