package playground.sergioo.visualizer2D2012;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.Painter;

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
	private class Scale {
		double startHeight;
		double endHeight;
		double startValue;
		double endValue;
		int divisions;
		double baseSize;
		Font font;
		NumberFormat format;
		Stroke stroke;
		Color color;
		private Scale(double startHeight, double endHeight, double startValue, double endValue, int divisions, double baseSize, Font font, NumberFormat format, Stroke stroke, Color color) {
			this.startHeight = startHeight;
			this.endHeight = endHeight;
			this.startValue = startValue;
			this.endValue = endValue;
			this.divisions = divisions;
			this.baseSize = baseSize;
			this.font = font;
			this.format = format;
			this.stroke = stroke;
			this.color = color;
		}
	}

	//Attributes
	private SortedMap<Double, Line> linesMap = new TreeMap<Double, Line>();
	private Scale scale;
	private Camera3D camera;
	private double side=30;
	
	//Constructors
	public LinesPainter3D() {
		super();
	}
	/**
	 * @param side
	 */
	public LinesPainter3D(double side) {
		super();
		this.side = side;
	}
	
	//Methods
	public void setScale(double startHeight, double endHeight, double startValue, double endValue, int divisions, double baseSize, Font font, NumberFormat format, Stroke stroke, Color color) {
		scale = new Scale(startHeight, endHeight, startValue, endValue, divisions, baseSize, font, format, stroke, color);
	}
	public void addLine(double[] pointA, double[] pointB, Color color, Stroke stroke) {
		double distance = camera==null?linesMap.size():camera.getDistanceToCamera(pointA);
		while(linesMap.get(-distance)!=null)
			distance-=0.001;
		linesMap.put(-distance, new Line(pointA, pointB, color, stroke));
	}
	public void clearLines() {
		linesMap.clear();
	}
	public void setCamera(Camera3D camera) {
		boolean refresh = false;
		if(this.camera==null)
			refresh = true;
		else if(!camera.getClass().equals(this.camera.getClass()))
			refresh = true;
		this.camera = camera;
		if(refresh)
			refreshLinesOrder();
	}
	public void refreshLinesOrder() {
		Collection<Line> lines = new ArrayList<Line>();
		for(Line line:linesMap.values()) {
			if(line.color!=null)
				lines.add(line);
		}
		linesMap.clear();
		for(Line line:lines)
			addLine(line.pointA, line.pointB, line.color, line.stroke);
		if(scale!=null && camera!=null)
			addLine(camera.getCenter(), camera.getCenter(), null, null);
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		for(Entry<Double, Line> line:linesMap.entrySet())
			if(line.getValue().color==null)
				paintVertical3DScale(g2, layersPanel, scale.startHeight, scale.endHeight, scale.startValue, scale.endValue, scale.divisions, scale.baseSize, scale.font, scale.format, scale.stroke, scale.color);
			else if(line.getKey()<0) {
				double x = line.getValue().pointA[0];
				double y = line.getValue().pointA[1];
				double zA = line.getValue().pointA[2];
				double zB = line.getValue().pointB[2];
				if(layersPanel.getCamera().isInside(new double[]{x,y,zA}) || layersPanel.getCamera().isInside(new double[]{x,y,zB})) {
					double[][] sideA = new double[][]{{x+side, y-side, zA}, {x+side, y+side, zA}, {x+side, y+side, zB}, {x+side, y-side, zB}};
					paintPolygon(g2, layersPanel, sideA, line.getValue().color.darker());
					double[][] sideB = new double[][]{{x+side, y+side, zA}, {x-side, y+side, zA}, {x-side, y+side, zB}, {x+side, y+side, zB}};
					paintPolygon(g2, layersPanel, sideB, line.getValue().color);
					double[][] sideC = new double[][]{{x-side, y+side, zA}, {x-side, y-side, zA}, {x-side, y-side, zB}, {x-side, y+side, zB}};
					paintPolygon(g2, layersPanel, sideC, line.getValue().color);
					double[][] sideD = new double[][]{{x-side, y-side, zA}, {x+side, y-side, zA}, {x+side, y-side, zB}, {x-side, y-side, zB}};
					paintPolygon(g2, layersPanel, sideD, line.getValue().color.darker());
					double[][] up = new double[][]{{x-side, y-side, zB}, {x+side, y-side, zB}, {x+side, y+side, zB}, {x-side, y+side, zB}};
					paintPolygon(g2, layersPanel, up, line.getValue().color);
				}
			}
	}

}
