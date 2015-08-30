package gunnar.ihop2.regent.demandreading;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import patryk.popgen2.Building;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * 
 * @author Gunnar Flötteröd, based on Patryk Larek
 *
 */
public class ZonalSystem {

	// -------------------- MEMBERS --------------------

	private final Map<String, Zone> id2zone = new LinkedHashMap<String, Zone>();

	// -------------------- CONSTRUCTION --------------------

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

	public void addBuildings(final String buildingShapeFileName) {

		final GeometryFactory geometryFactory = new GeometryFactory();
		final WKTReader wktReader = new WKTReader(geometryFactory);

		final ShapeFileReader shapeFileReader = new ShapeFileReader();
		shapeFileReader.readFileAndInitialize(buildingShapeFileName);
		final Collection<SimpleFeature> features = shapeFileReader
				.getFeatureSet();

		for (SimpleFeature ft : features) {

			try {
				final Geometry geometry = wktReader.read((ft
						.getAttribute("the_geom")).toString());
				final String buildingType = ft.getAttribute("ANDAMAL_1T")
						.toString();
				final int buildingSize = Integer.valueOf(ft
						.getAttribute("AREA").toString());
				final Building building = new Building(geometry, buildingSize);
				building.setBuildingType(buildingType);

				for (Zone zone : this.id2zone.values()) {
					if (zone.getGeometry() != null
							&& zone.getGeometry().intersects(geometry)) {
						zone.addBuilding(building);
						break;
					}
				}

			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}

	// -------------------- CONTENT ACCESS --------------------

	public Zone getZone(final String id) {
		return this.id2zone.get(id);
	}

	public Map<String, Zone> getZonesInsideBoundary(
			final String zonesBoundaryShapeFileName) {

		final Collection<SimpleFeature> features = ShapeFileReader
				.getAllFeatures(zonesBoundaryShapeFileName);
		if (features.size() != 1) {
			throw new RuntimeException("not exactly one feature in shape file");
		}

		final SimpleFeature feature = features.iterator().next();
		final WKTReader wktreader = new WKTReader();
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

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		final String zonesShapefile = "./data/shapes/sverige_TZ_EPSG3857.shp";
		// final String zonesBoundaryShape = "./data/shapes/limit_EPSG3857.shp";

		System.out.println("STARTED ...");

		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapefile);
		// for (String zoneId : zonalSystem.getZonesInsideBoundary(
		// zonesBoundaryShape).keySet()) {
		for (String zoneId : zonalSystem.id2zone.keySet()) {
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
