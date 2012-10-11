package playground.demandde.pendlermatrix;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;




public class Verschmierer {
	
	private String filename;
	
	private Map<Integer, Geometry> zones = new HashMap<Integer, Geometry>();

	private Random random = new Random();
	
	public Verschmierer(String filename) {
		this.filename = filename;
		readShape();
	}

	@SuppressWarnings("unchecked")
	private void readShape() {
		FeatureSource landkreisSource;
		try {
			landkreisSource = ShapeFileReader.readDataFile(filename);
			landkreisSource.getFeatures();
			Collection<Feature> landkreise = landkreisSource.getFeatures();
			for (Feature landkreis : landkreise) {
				Integer gemeindeschluessel = Integer.parseInt((String) landkreis.getAttribute("gemeindesc"));
				zones.put(gemeindeschluessel, landkreis.getDefaultGeometry());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Geometry findZone(Coord coord) {
		GeometryFactory gf = new GeometryFactory();
		Point point = gf.createPoint(new Coordinate(coord.getX(), coord.getY()));
		for (Geometry zone : zones.values()) {
			if (zone.contains(point)) {
				return zone;
			}
		}
		return null;
	}

	public Coord shootIntoSameZoneOrLeaveInPlace(Coord coord) {
		Geometry zone = findZone(coord);
		if (zone != null) {
			return doShoot(zone);
		} else {
			return coord;
		}
	}
	
	private static Point getRandomPointInFeature(Random rnd, Geometry g) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		return p;
	}
	
	private Coord doShoot(Geometry zone) {
		Coord coord;
		Point point = getRandomPointInFeature(this.random , zone);
		coord = new CoordImpl(point.getX(), point.getY());
		return coord;
	}
	
	
}
