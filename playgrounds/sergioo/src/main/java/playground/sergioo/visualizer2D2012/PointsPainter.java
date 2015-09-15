package playground.sergioo.visualizer2D2012;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;

public class PointsPainter extends Painter {
	
	//Enum
	public enum TypePoint {
		X,
		O;
	}
	
	//Attributes
	private Collection<Coord> points = new ArrayList<Coord>();
	private Coord selectedPoint;
	private Color color;
	private TypePoint type = TypePoint.X;
	private double pointSize;
	
	//Methods
	public PointsPainter() {
		this(Color.BLACK);
	}
	public PointsPainter(Color color) {
		super();
		this.color = color;
	}
	public Collection<Coord> getPoints() {
		return points;
	}
	public void addPoint(Coord point) {
		points.add(point);
	}
	public void clearPoints() {
		points.clear();
	}
	public void setType(TypePoint type) {
		this.type = type;
	}
	public void setPointSize(double pointSize) {
		this.pointSize = pointSize;
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		if(type.equals(TypePoint.O))
			for(Coord point:points)
				paintCircle(g2, layersPanel, point, (int)pointSize, color);
		else
			for(Coord point:points)
				paintX(g2, layersPanel, point, pointSize, color);
		if(selectedPoint!=null)
			paintCircle(g2, layersPanel, selectedPoint, 4, Color.RED);
	}
	public void selectPoint(Coord point) {
		if(points.contains(point))
			selectedPoint = point;
	}

}
