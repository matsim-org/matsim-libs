package patryk.popgen2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import gunnar.ihop2.regent.demandreading.Zone;

public class SelectZones {
	private String polygonFile;
	private Map<String, Zone> zones;
	private Geometry limitingPolygon;

	public SelectZones(Map<String, Zone> zones, String filename) {
		this.zones = zones;
		this.polygonFile = filename;
	}

	public ArrayList<String> getZonesInsideBoundary() {
		ArrayList<String> coveredZones = new ArrayList<>();
		readLimitingPolygonSHP(polygonFile);

		for (String key : zones.keySet()) {
			Geometry polygon = zones.get(key).getGeometry();
			if (limitingPolygon.covers(polygon)) {
				coveredZones.add(key);
			}
		}
		return coveredZones;
	}

	private void readLimitingPolygonSHP(String filename) {
		Collection<SimpleFeature> featureCollection = ShapeFileReader
				.getAllFeatures(filename);
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>(
				featureCollection);

		if (!features.isEmpty()) {
			if (features.size() > 1) {
				System.out.println("More than one feature in shapefile.");
				System.exit(-1);
			}
			SimpleFeature feature = features.get(0);
			WKTReader wktreader = new WKTReader();
			try {
				Geometry geometry = wktreader.read(feature.getAttribute(
						"the_geom").toString());
				limitingPolygon = geometry;
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		} else {
			System.out.println("No features loaded from shapefile.");
		}
	}
}
