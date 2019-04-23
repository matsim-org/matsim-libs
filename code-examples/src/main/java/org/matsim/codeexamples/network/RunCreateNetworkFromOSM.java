package org.matsim.codeexamples.network;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.opengis.feature.simple.SimpleFeature;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Example on how to convert osm data from e.g. http://download.geofabrik.de into a MATSim network. This examle puts all
 * motorways and primary roads into the MATSim network. If a link is contained in the supplied shape, also minor and
 * residential raods are put into the MATsim network.
 * <p>
 * After parsing the OSM-data, unreachable areas of the network are removed by using the network cleaner
 */
public class RunCreateNetworkFromOSM {

	private static String UTM32nAsEpsg = "EPSG:25832";
	private static Path input = Paths.get("/path/to/your/input/data.osm");
	private static Path filterShape = Paths.get("/path/to/your/filter/shape-file.shp");

	public static void main(String[] args) {
		new RunCreateNetworkFromOSM().create();
	}

	private void create() {

		// create an empty network which will contain the parsed network data
		Network network = NetworkUtils.createNetwork();

		// choose an appropriate coordinate transformation. OSM Data is in WGS84. When working in central Germany,
		// EPSG:25832 or EPSG:25833 as target system is a good choice
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, UTM32nAsEpsg
		);

		// create an osm network reader with a filter
		OsmNetworkReader reader = new OsmNetworkReader(network, transformation, true, true);
		reader.addOsmFilter(new NetworkFilter(filterShape));

		// the actual work is done in this call. Depending on the data size this may take a long time
		reader.parse(input.toString());

		// clean the network to remove unconnected parts where agents might get stuck
		new NetworkCleaner().run(network);

		// write out the network into a file
		new NetworkWriter(network).write("/path/to/your/output/network.xml.gz");
	}

	/**
	 * Includes motorways and primary roads for the whole dataset. If roads are contained within the supplied shape, also
	 * secondary and residential roads are included
	 */
	private static class NetworkFilter implements OsmNetworkReader.OsmFilter {

		private final Collection<Geometry> geometries = new ArrayList<>();

		NetworkFilter(Path shapeFile) {
			for (SimpleFeature feature : ShapeFileReader.getAllFeatures(shapeFile.toString())) {
				geometries.add((Geometry) feature.getDefaultGeometry());
			}
		}

		@Override
		public boolean coordInFilter(Coord coord, int hierarchyLevel) {
			// hierachy levels 1 - 3 are motorways and primary roads, as well as their trunks
			if (hierarchyLevel <= 4) return true;

			// if coord is within the supplied shape use every street above level of tracks and cycle ways
			return hierarchyLevel <= 8 && containsCoord(coord);
		}

		private boolean containsCoord(Coord coord) {
			return geometries.stream().anyMatch(geom -> geom.contains(MGC.coord2Point(coord)));
		}
	}
}
