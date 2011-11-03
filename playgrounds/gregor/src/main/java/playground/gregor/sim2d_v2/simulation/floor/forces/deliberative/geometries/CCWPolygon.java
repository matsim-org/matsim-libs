package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.geometries;


import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class CCWPolygon {

	private final Polygon geometry;
	private final Coordinate[] ring;

	private final GeometryFactory geofac = new GeometryFactory();
	private final Coordinate[] reflectedRing;

	public CCWPolygon(Coordinate [] coords, Coordinate refCoord) {
		Coordinate [] ccw = getCCWRing(coords);

		this.geometry = this.geofac.createPolygon(this.geofac.createLinearRing(ccw), null);
		this.ring = ccw;

		this.reflectedRing = getReflectedRing(ccw, refCoord);
	}

	private Coordinate[] getReflectedRing(Coordinate[] ccw, Coordinate refCoord) {
		Coordinate [] ret = new Coordinate[ccw.length];
		for (int i = 0; i < ccw.length; i++) {
			Coordinate c = ccw[i];
			ret[i]= new Coordinate(-(c.x - refCoord.x),-(c.y - refCoord.y));
		}
		return getCCWRing(ret);
	}

	public Polygon getCCWPolygon() {
		return this.geometry;
	}

	public Coordinate [] getCCWRing() {
		return this.ring;
	}

	public Coordinate [] getReflectedCCWRing() {
		return this.reflectedRing;
	}

	private Coordinate [] getCCWRing(Coordinate[] coords) {

		if (coords[0].distance(coords[coords.length-1]) > 0) {
			throw new RuntimeException("Coordinate array must be a ring!");
		}

		if (!CGAlgorithms.isCCW(coords)) {
			reverseCoords(coords);
		}

		startWithSmallestCoordinate(coords);


		return coords;
	}

	private void startWithSmallestCoordinate(Coordinate[] coords) {
		Coordinate [] tmp = new Coordinate[coords.length];

		int startP = 0;
		double minY = Double.POSITIVE_INFINITY;
		double minX = Double.POSITIVE_INFINITY;
		for (int i = 0; i < coords.length-1; i++) {
			if (coords[i].y < minY) {
				minY = coords[i].y;
				minX = coords[i].x;
				startP = i;
			} else if (coords[i].y == minY && coords[i].x < minX) {
				minY = coords[i].y;
				minX = coords[i].x;
				startP = i;
			}
		}

		for (int i = 0; i < coords.length-1; i++) {
			tmp[i] = coords[startP++];
			if (startP == coords.length-1) {
				startP = 0;
			}
		}
		tmp[coords.length-1] = tmp[0];


		for (int i = 0; i < coords.length; i++) {
			coords[i] = tmp[i];
		}
	}

	private void reverseCoords(Coordinate[] coords) {
		int i = 0;
		int j = coords.length -1;
		while (i < j) {
			swap(coords, i++, j--);
		}

	}

	private void swap(Coordinate[] coords, int i, int j) {
		Coordinate tmp = coords[i];
		coords[i] = coords[j];
		coords[j] = tmp;

	}

}
