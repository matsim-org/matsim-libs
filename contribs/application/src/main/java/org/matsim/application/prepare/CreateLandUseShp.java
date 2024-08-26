package org.matsim.application.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFactorySpi;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import picocli.CommandLine;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@CommandLine.Command(
		name = "create-landuse-shp",
		description = "Create a shapefile of landuse"
)
public class CreateLandUseShp implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateLandUseShp.class);
	private static final Set<String> INCLUDE = Set.of(
			"commercial",
			"construction",
			"industrial",
			"residential",
			"retail",
			"allotments",
			"military",
			"recreation_ground",
			"park",
			"building"
	);

	@CommandLine.Parameters(paramLabel = "INPUT", arity = "1..*", description = "Path to input shapes")
	private List<Path> inputs;

	@CommandLine.Option(names = "--layer", arity = "0..*", description = "Name of the shape file if input is a zip")
	private List<String> layer = List.of("gis_osm_landuse_a_free_1.shp", "gis_osm_buildings_a_free_1.shp");

	@CommandLine.Option(names = "--attr", description = "Name of attribute to match", defaultValue = "fclass")
	private String attr;

	@CommandLine.Option(names = "--output", description = "Output path for shape file", required = true)
	private Path output;

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions();

	@Override
	public Integer call() throws Exception {

		log.info("Reading landuse data from {}", inputs);

		if (!inputs.stream().allMatch(Files::exists)) {
			log.error("One of Input files does not exist: {}", inputs);
			return 2;
		}

		if (crs.getInputCRS() != null) {
			log.warn("input-crs={} will be ignored, only target-crs can be set", crs.getInputCRS());
		}

		FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();

		DataStore out = factory.createNewDataStore(Collections.singletonMap("url", output.toUri().toURL()));
		SimpleFeatureType schema = null;

		for (Path input : inputs) {

			log.info("Reading {}...", input);

			// index to avoid redundant shapes
			STRtree index = new STRtree();
			boolean built = false;

			List<Path> paths =new ArrayList<>();
			if (input.toString().endsWith("zip")) {
				FileSystem fs = FileSystems.newFileSystem(input, ClassLoader.getSystemClassLoader());
				for (String l : layer) {
					paths.add(fs.getPath(l));
				}

			} else if (input.toString().endsWith("shp")) {
				paths.add(input);
			} else
				throw new IllegalArgumentException("Unknown file format: " + input);

			// go through multiple layers
			for (Path path : paths) {
				FileDataStore ds = factory.createDataStore(path.toUri().toURL());

				ListFeatureCollection list = new ListFeatureCollection(ds.getSchema());

				try (var reader = ds.getFeatureReader()) {

					outer: while (reader.hasNext()) {
						SimpleFeature ft = reader.next();
						String type = (String) ft.getAttribute(attr);
						if (INCLUDE.contains(type)) {
							MultiPolygon geom = (MultiPolygon) ft.getDefaultGeometry();

							// build an index with the first layer
							if (built) {
								List<MultiPolygon> result = index.query(geom.getEnvelopeInternal());

								// if this polygon is fully contained by another one we don't need it
								for (MultiPolygon p : result) {
									if (p.contains(geom))
										continue outer;
								}

							} else
								index.insert(geom.getEnvelopeInternal(), geom);

							list.add(ft);
						}
					}
				}

				if (!built) {
					index.build();
					built = true;
				}

				if (schema == null) {
					schema = ds.getSchema();
					if (crs.getTargetCRS() != null) {
						log.info("Reprojecting to {}", crs.getTargetCRS());
						schema = SimpleFeatureTypeBuilder.retype(ds.getSchema(), CRS.decode(crs.getTargetCRS()));
					}

					out.createSchema(schema);
				}

				String typeName = out.getTypeNames()[0];
				SimpleFeatureStore featureStore = (SimpleFeatureStore) out.getFeatureSource(typeName);

				if (crs.getTargetCRS() == null) {
					featureStore.addFeatures(list);
				} else {
					ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(list, schema.getCoordinateReferenceSystem());
					featureStore.addFeatures(rfc);
				}

				ds.dispose();
			}
		}

		out.dispose();

		return 0;
	}

}
