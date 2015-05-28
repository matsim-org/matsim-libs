package gunnar.ihop2;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import patryk.popgen2.Zone;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ZonalSystem {

	// MEMBERS

	private final Map<String, Zone> id2zone = new LinkedHashMap<String, Zone>();

	// CONSTRUCTION

	public ZonalSystem(final String zonesShapeFileName) {
		final GeometryFactory geometryFactory = new GeometryFactory();
		final WKTReader wktReader = new WKTReader(geometryFactory);
		for (SimpleFeature ft : ShapeFileReader
				.getAllFeatures(zonesShapeFileName)) {
			try {
				final String zoneId = ft.getAttribute("ZONE").toString();
				final Zone zone = new Zone(zoneId);
				zone.setGeometry(wktReader.read((ft.getAttribute("the_geom"))
						.toString()));
				this.id2zone.put(zoneId, zone);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public Map<String, Zone> getZones() {
		return this.id2zone;
	}

	public Map<String, Zone> getZonesInsideBoundary(
			final String zonesBoundaryShape) {
		final Collection<SimpleFeature> features = ShapeFileReader
				.getAllFeatures(zonesBoundaryShape);

		if (features.size() != 1) {
			throw new RuntimeException("not exactly one feature in shape file");
		}

		SimpleFeature feature = features.iterator().next();
		WKTReader wktreader = new WKTReader();
		final Geometry limitingPolygon;
		try {
			limitingPolygon = wktreader.read(feature.getAttribute("the_geom")
					.toString());
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		final Map<String, Zone> result = new LinkedHashMap<String, Zone>();
		for (Map.Entry<String, Zone> id2zoneEntry : this.id2zone.entrySet()) {
			if (limitingPolygon.covers(id2zoneEntry.getValue().getGeometry())) {
				result.put(id2zoneEntry.getKey(), id2zoneEntry.getValue());
			}
		}
		return result;
	}

	// MAIN-FUNCTION, ONLY FOR TESTING

	public static void main(String[] args) {

		final String zonesShapefile = "./data/shapes/sverige_TZ_EPSG3857.shp";
		final String zonesBoundaryShape = "./data/shapes/limit_EPSG3857.shp";

		System.out.println("STARTED ...");

		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapefile);
		// for (String zoneId : zonalSystem.getZonesInsideBoundary(
		// zonesBoundaryShape).keySet()) {
		for (String zoneId : zonalSystem.getZones().keySet()) {
			System.out.println("  <zone value=\"" + zoneId + "\"/>");
		}

		// System.out.println("total number of zones: "
		// + zonalSystem.getZones().size());
		// System.out
		// .println("truncated number of zones: "
		// + zonalSystem
		// .getZonesInsideBoundary(zonesBoundaryShape)
		// .size());
		//
		System.out.println("... DONE");
	}

}
