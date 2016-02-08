package playground.vaadinexample;

import com.vividsolutions.jts.geom.Geometry;

public class Contour {

	private double z;
	private Geometry geometry;
	private String color;

	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getColor() {
		return color;
	}

	public double getArea() {
		return geometry.getArea();
	}

	public int getNGeometries() {
		return geometry.getNumGeometries();
	}

}
