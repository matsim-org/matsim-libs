package org.matsim.application.prepare.network;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class collects subnetworks of links that are connected by certain types.
 */
@CommandLine.Command(name = "extract-neighbourhoods", description = "Extracts neighbourhoods from a network.", showDefaultValues = true)
@CommandSpec(
	requireNetwork = true,
	produces = {"network.xml.gz", "neighbourhoods.shp"}
)
public class ExtractNeighbourhoods implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(ExtractNeighbourhoods.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(ExtractNeighbourhoods.class);

	@CommandLine.Option(names = "--road-types", description = "Residential road types to consider for neighbourhood extraction", split = ",",
		defaultValue = "residential,living_street,service")
	private Set<String> roadTypes;

	@CommandLine.Option(names = "--buffer", description = "Negative buffer around neighbourhoods", defaultValue = "5.0")
	private double buffer;

	public static void main(String[] args) {
		new ExtractNeighbourhoods().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Network network = input.getNetwork();

		Int2ObjectMap<Set<Id<Link>>> mapping = extractNeighbourhoods(network);

		createShapefile(output.getPath("neighbourhoods.shp"), network, mapping);

		return 0;
	}

	private Int2ObjectMap<Set<Id<Link>>> extractNeighbourhoods(Network network) {

		Object2IntMap<Id<Link>> assigned = new Object2IntOpenHashMap<>();
		Int2ObjectMap<Set<Id<Link>>> result = new Int2ObjectArrayMap<>();

		for (Link link : network.getLinks().values()) {

			// Check if already detected
			if (assigned.containsKey(link.getId())) {
				continue;
			}

			// Only collect residential links
			if (!roadTypes.contains(NetworkUtils.getHighwayType(link))) {
				continue;
			}

			Set<Id<Link>> neighbourhood = new HashSet<>();
			search(neighbourhood, link);

			Link opposite = NetworkUtils.findLinkInOppositeDirection(link);
			if (opposite != null)
				search(neighbourhood, opposite);

			int idx = result.size();
			result.put(idx, neighbourhood);

			for (Id<Link> id : neighbourhood) {
				assigned.put(id, idx);
			}
		}

		return result;
	}

	/**
	 * Recursive search for link that are connected by certain road type.
	 */
	private void search(Set<Id<Link>> neighbourhood, Link link) {

		neighbourhood.add(link.getId());

		// Check if node is connected to any other non-residential link type
		boolean allowed = true;
		for (Link out : link.getToNode().getOutLinks().values()) {
			String highwayType = NetworkUtils.getHighwayType(out);
			if (!roadTypes.contains(highwayType)) {
				allowed = false;
				break;
			}
		}
		for (Link in : link.getToNode().getInLinks().values()) {
			String highwayType = NetworkUtils.getHighwayType(in);
			if (!roadTypes.contains(highwayType)) {
				allowed = false;
				break;
			}
		}

		if (allowed) {
			for (Link out : link.getToNode().getOutLinks().values()) {
				if (!neighbourhood.contains(out.getId())) {
					search(neighbourhood, out);
				}
			}
			for (Link in : link.getToNode().getInLinks().values()) {
				if (!neighbourhood.contains(in.getId())) {
					search(neighbourhood, in);
				}
			}
		}
	}

	private void createShapefile(Path path, Network network, Int2ObjectMap<Set<Id<Link>>> mapping) throws FactoryException, IOException {

		String crs = ProjectionUtils.getCRS(network);

		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("schema");
		if (crs != null)
			typeBuilder.setCRS(CRS.decode(crs));

		typeBuilder.add("the_geom", Polygon.class);

		SimpleFeatureType featureType = typeBuilder.buildFeatureType();

		FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();
		ShapefileDataStore ds = (ShapefileDataStore) factory.createNewDataStore(Map.of("url", path.toFile().toURI().toURL()));
		ds.createSchema(featureType);

		SimpleFeatureStore source = (SimpleFeatureStore) ds.getFeatureSource();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		DefaultFeatureCollection collection = new DefaultFeatureCollection(null, featureType);

		Transaction transaction = new DefaultTransaction("create");
		source.setTransaction(transaction);

		// Add features to shp file
		GeometryFactory f = JTSFactoryFinder.getGeometryFactory();
		for (Int2ObjectMap.Entry<Set<Id<Link>>> e : mapping.int2ObjectEntrySet()) {

			LineString[] lines = e.getValue().stream()
				.map(id -> network.getLinks().get(id))
				.map(link -> f.createLineString(new Coordinate[]{
					new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()),
					new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY())
				}))
				.toArray(LineString[]::new);

			Geometry hull = f.createGeometryCollection(lines).union().convexHull();
			featureBuilder.add(hull);

			SimpleFeature ft = featureBuilder.buildFeature(String.valueOf(e.getIntKey()));
			collection.add(ft);
		}


		// Close and write shape file
		source.addFeatures(collection);
		transaction.commit();
		transaction.close();

		ds.dispose();
	}
}
