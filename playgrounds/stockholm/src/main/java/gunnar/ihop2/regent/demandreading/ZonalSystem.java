package gunnar.ihop2.regent.demandreading;

import static java.lang.Math.max;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import patryk.popgen2.Building;
import saleem.stockholmmodel.utils.StockholmTransformationFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * 
 * @author Gunnar Flötteröd, based on Patryk Larek
 *
 */
public class ZonalSystem implements Iterable<Zone> {

	// -------------------- MEMBERS --------------------

	private final String zonalCoordinateSystem;

	private final Map<String, Zone> id2zone = new LinkedHashMap<String, Zone>();

	private Map<Node, Zone> node2zone = null;

	private Map<Zone, Set<Node>> zone2nodes = null;

	// -------------------- CONSTRUCTION --------------------

	public ZonalSystem(final String zonesShapeFileName,
			final String zonalCoordinateSystem) {
		this.zonalCoordinateSystem = zonalCoordinateSystem;
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

	public void addNetwork(final Network network,
			final String networkCoordinateSystem) {

		final CoordinateTransformation node2zoneCoordinateTrafo = StockholmTransformationFactory
				.getCoordinateTransformation(networkCoordinateSystem,
						this.zonalCoordinateSystem);

		this.node2zone = new LinkedHashMap<Node, Zone>();
		this.zone2nodes = new LinkedHashMap<Zone, Set<Node>>();

		for (Node node : network.getNodes().values()) {
			for (Zone zone : this.id2zone.values()) {
				if (zone.getGeometry().contains(
						MGC.coord2Point(node2zoneCoordinateTrafo.transform(node
								.getCoord())))) {
					this.node2zone.put(node, zone);
					Set<Node> nodeSet = zone2nodes.get(zone);
					if (nodeSet == null) {
						nodeSet = new LinkedHashSet<Node>();
						zone2nodes.put(zone, nodeSet);
					}
					nodeSet.add(node);
					break;
				}
			}
		}
	}

	public int[] getNodePerZoneAbsFreqs() {
		int maxNodeCnt = 0;
		for (Set<Node> nodes : this.zone2nodes.values()) {
			maxNodeCnt = max(maxNodeCnt, nodes.size());
		}
		final int[] absoluteFrequencies = new int[maxNodeCnt + 1];
		for (Set<Node> nodes : this.zone2nodes.values()) {
			absoluteFrequencies[nodes.size()]++;
		}
		return absoluteFrequencies;
	}

	public Zone getZone(final Node node) {
		if (this.node2zone == null) {
			return null;
		} else {
			return this.node2zone.get(node);
		}
	}

	public Set<Node> getNodes(final Node node) {
		if (this.zone2nodes == null) {
			return null;
		} else {
			return this.zone2nodes.get(node);
		}
	}

	// TODO NEW
	public Set<Node> getNodes(final Zone zone) {
		Set<Node> result = this.zone2nodes.get(zone);
		if (result == null) {
			result = new LinkedHashSet<>();
		}
		return result;
	}

	// TODO NEW
	public Set<Node> getNodes(final String zoneId) {
		if (this.id2zone.containsKey(zoneId)) {
			return (this.getNodes(this.id2zone.get(zoneId)));
		} else {
			return new LinkedHashSet<>();
		}
	}

	// -------------------- CONTENT ACCESS --------------------

	public Map<String, Zone> getId2zoneView() {
		return Collections.unmodifiableMap(this.id2zone);
	}

	public Zone getZone(final String id) {
		return this.id2zone.get(id);
	}

	// public Map<String, Zone> getZonesInsideBoundary(
	// final String zonesBoundaryShapeFileName) {
	//
	// final Collection<SimpleFeature> features = ShapeFileReader
	// .getAllFeatures(zonesBoundaryShapeFileName);
	// if (features.size() != 1) {
	// throw new RuntimeException("not exactly one feature in shape file");
	// }
	//
	// final SimpleFeature feature = features.iterator().next();
	// final WKTReader wktreader = new WKTReader();
	// final Geometry limitingPolygon;
	// try {
	// limitingPolygon = wktreader.read(feature.getAttribute("the_geom")
	// .toString());
	// } catch (ParseException e) {
	// throw new RuntimeException(e);
	// }
	//
	// final Map<String, Zone> result = new LinkedHashMap<String, Zone>();
	// for (Map.Entry<String, Zone> id2zoneEntry : this.id2zone.entrySet()) {
	// if (limitingPolygon.covers(id2zoneEntry.getValue().getGeometry())) {
	// result.put(id2zoneEntry.getKey(), id2zoneEntry.getValue());
	// }
	// }
	// return result;
	// }

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String zonesShapefile = "./data/shapes/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapefile,
				StockholmTransformationFactory.WGS84_EPSG3857);

		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile",
				"./data/transmodeler/network.xml");
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		zonalSystem.addNetwork(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);
		final int[] absSizeFreqs = zonalSystem.getNodePerZoneAbsFreqs();
		System.out.println("nodeCnt\tabsFreq");
		for (int i = 0; i < absSizeFreqs.length; i++) {
			System.out.println(i + "\t" + absSizeFreqs[i]);
		}

		System.out.println("... DONE");
	}

	// TODO NEW
	@Override
	public Iterator<Zone> iterator() {
		return this.id2zone.values().iterator();
	}
}
