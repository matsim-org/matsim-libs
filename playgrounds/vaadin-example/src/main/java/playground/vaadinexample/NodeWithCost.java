package playground.vaadinexample;

import com.vividsolutions.jts.geom.Point;

public class NodeWithCost {

	private Point geometry;
	private double time;
	private double cost;
	private String color;

	public void setGeometry(Point geometry) {
		this.geometry = geometry;
	}

	public Point getGeometry() {
		return geometry;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public double getTime() {
		return time;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public double getCost() {
		return cost;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getColor() {
		return color;
	}
}
