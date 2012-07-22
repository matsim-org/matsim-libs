package playground.sergioo.Visualizer2D;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math.geometry.Vector3D;
import org.matsim.core.utils.collections.Tuple;

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
	private Camera3DOrtho camera;
	
	//Methods
	public void addLine(double[] pointA, double[] pointB, Color color, Stroke stroke) {
		Vector3D center = new Vector3D((pointA[0]+pointB[0])/2, (pointA[1]+pointB[1])/2,(pointA[2]+pointB[2])/2); 
		double distance = camera==null?0:camera.getDistanceToCamera(center);
		linesMap.put(-distance, new Line(pointA, pointB, color, stroke));
	}
	public void clearLines() {
		linesMap.clear();
	}
	public void setCamera(Camera3DOrtho camera) {
		boolean refresh = false;
		if(camera==null)
			refresh = true;
		this.camera = camera;
		if(refresh)
			refreshLinesOrder();
	}
	public void refreshLinesOrder() {
		Collection<Line> lines = linesMap.values();
		linesMap.clear();
		for(Line line:lines)
			addLine(line.pointA, line.pointB, line.color, line.stroke);
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		Stroke stroke = new BasicStroke(2);
		for(Entry<Double, Line> line:linesMap.entrySet())
			if(line.getKey()>0)
				paintLine(g2, layersPanel, line.getValue().pointA, line.getValue().pointB, line.getValue().stroke, line.getValue().color);
	}

}
