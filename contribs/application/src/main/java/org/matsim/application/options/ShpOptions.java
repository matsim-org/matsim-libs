package org.matsim.application.options;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.AbstractNode;
import org.locationtech.jts.index.strtree.Boundable;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Reusable class for shape file options.
 *
 * @see CommandLine.Mixin
 */
public final class ShpOptions {

	private static final Logger log = LogManager.getLogger(ShpOptions.class);

	@CommandLine.Option(names = "--shp", description = "Optional path to shape file used for filtering", required = false)
	private Path shp;

	@CommandLine.Option(names = "--shp-crs", description = "Overwrite coordinate system of the shape file")
	private String shpCrs;

	@CommandLine.Option(names = "--shp-charset", description = "Charset used to read the shape file", defaultValue = "ISO-8859-1")
	private Charset shpCharset;

	public ShpOptions() {
	}

	/**
	 * Constructor to use shape options manually.
	 */
	public ShpOptions(Path shp, @Nullable String shpCrs, @Nullable Charset shpCharset) {
		this.shp = shp;
		this.shpCrs = shpCrs;
		this.shpCharset = shpCharset;
	}

	/**
	 * Get the provided input crs.
	 */
	public Path getShapeFile() {
		return shp;
	}

	/**
	 * Get the provided target crs.
	 */
	public String getShapeCrs() {
		return detectCRS();
	}

	/**
	 * Read features from configured shape file.
	 *
	 * @return null if no shp configured.
	 */
	public List<SimpleFeature> readFeatures() {
		if (shp == null)
			throw new IllegalStateException("Shape file path not specified");
		if (!Files.exists(shp))
			throw new IllegalStateException(String.format("Shape file %s does not exists", shp));

		try {
			ShapefileDataStore ds = openDataStore(shp);
			if (shpCharset != null)
				ds.setCharset(shpCharset);

			return ShapeFileReader.getSimpleFeatures(ds);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Opens datastore to a shape-file.
	 */
	public static ShapefileDataStore openDataStore(Path shp) throws IOException {

		FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();

		ShapefileDataStore ds;
		if (shp.toString().endsWith(".shp"))
			ds = (ShapefileDataStore) factory.createDataStore(shp.toUri().toURL());
		else if (shp.toString().endsWith(".zip")) {

			FileSystem fs = FileSystems.newFileSystem(shp, ClassLoader.getSystemClassLoader());
			Optional<Path> match = Files.walk(fs.getPath("/"))
					.filter(p -> p.toString().endsWith(".shp"))
					.findFirst();

			if (match.isEmpty())
				throw new IllegalArgumentException("No .shp file found in the zip.");

			log.info("Using {} from {}", match.get(), shp);
			ds = (ShapefileDataStore) factory.createDataStore(match.get().toUri().toURL());
		} else {
			throw new IllegalArgumentException("Shape file must either be .zip or .shp, but was: " + shp);
		}

		return ds;
	}

	/**
	 * Return the union of all geometries in the shape file.
	 */
	public Geometry getGeometry() {

		Collection<SimpleFeature> features = readFeatures();
		if (features.size() < 1) {
			throw new IllegalStateException("There is no feature in the shape file. Aborting...");
		}
		Geometry geometry = (Geometry) features.iterator().next().getDefaultGeometry();
		if (features.size() > 1) {
			for (SimpleFeature simpleFeature : features) {
				Geometry subArea = (Geometry) simpleFeature.getDefaultGeometry();
				geometry.union(subArea);
			}
		}
		return geometry;
	}

	/**
	 * Creates an index to query features from this shape file.
	 *
	 * @param queryCRS coordinate system of the queries
	 * @param attr     the attribute to query from the shape file
	 * @param filter   filter features by attribute values
	 */
	public Index createIndex(String queryCRS, String attr, Set<String> filter) {

		if (shp == null)
			throw new IllegalStateException("Shape file path not specified");
		if (!Files.exists(shp))
			throw new IllegalStateException(String.format("Shape file %s does not exists", shp));
		if (queryCRS == null)
			throw new IllegalArgumentException("Input crs must not be null!");

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(queryCRS, detectCRS());

		try {
			return new Index(ct, attr, filter);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Create an index without a filter.
	 *
	 * @see #createIndex(String, String)
	 */
	public Index createIndex(String queryCRS, String attr) {
		return createIndex(queryCRS, attr, null);
	}

	/**
	 * Create a coordinate transformation to the shape file crs. Tries to autodetect the crs of the shape file.
	 */
	public CoordinateTransformation createTransformation(String fromCRS) {
		return TransformationFactory.getCoordinateTransformation(fromCRS, detectCRS());
	}

	private String detectCRS() {

		if (shpCrs == null) {
			try {
				ShapefileDataStore ds = openDataStore(shp);
				CoordinateReferenceSystem crs = ds.getSchema().getCoordinateReferenceSystem();
				ds.dispose();
				shpCrs = "EPSG:" + CRS.lookupEpsgCode(crs, true);
				log.info("Using detected crs for {}: {}", shp, shpCrs);

			} catch (IOException | FactoryException e) {
				throw new IllegalStateException("Could not determine crs of the shape file. Try to specify it manually using --shp-crs.", e);
			}
		}

		return shpCrs;
	}


	/**
	 * Helper class to provide an index for a shapefile lookup.
	 */
	public final class Index {

		private final STRtree index = new STRtree();
		private final CoordinateTransformation ct;
		private final String attr;

		/**
		 * Constructor.
		 *
		 * @param ct   coordinate transform from query to target crs
		 * @param attr attribute for the result of {@link #query(Coord)}
		 */
		Index(CoordinateTransformation ct, String attr, @Nullable Set<String> filter)
				throws IOException {
			ShapefileDataStore ds = openDataStore(shp);

			if (shpCharset != null)
				ds.setCharset(shpCharset);

			FeatureReader<SimpleFeatureType, SimpleFeature> it = ds.getFeatureReader();
			while (it.hasNext()) {
				SimpleFeature ft = it.next();

				if (filter != null && !filter.contains(ft.getAttribute(attr)))
					continue;

				Geometry geom = (Geometry) ft.getDefaultGeometry();
				Envelope env = geom.getEnvelopeInternal();
				index.insert(env, ft);
			}

			index.build();

			it.close();
			ds.dispose();

			//log.info("Created index with size: {}, depth: {}", index.size(), index.depth());

			this.ct = ct;
			this.attr = attr;
		}

		/**
		 * Query the index for first feature including a certain point.
		 *
		 * @return null when no features was found that contains the point
		 */
		@Nullable
		@SuppressWarnings("unchecked")
		public String query(Coord coord) {
			// Because we can not easily transform the feature geometry with MATSim we have to do it the other way around...
			Coordinate p = MGC.coord2Coordinate(ct.transform(coord));

			List<SimpleFeature> result = index.query(new Envelope(p));
			for (SimpleFeature ft : result) {
				Geometry geom = (Geometry) ft.getDefaultGeometry();
				if (geom.contains(MGC.coordinate2Point(p)))
					return (String) ft.getAttribute(attr);
			}

			return null;
			// throw new NoSuchElementException(String.format("No matching entry found for x:%f y:%f %s", x, y, p));
		}

		/**
		 * Checks whether a coordinate is contained in any of the features.
		 */
		public boolean contains(Coord coord) {

			Coordinate p = MGC.coord2Coordinate(ct.transform(coord));

			List<SimpleFeature> result = index.query(new Envelope(p));
			for (SimpleFeature ft : result) {
				Geometry geom = (Geometry) ft.getDefaultGeometry();
				if (geom.contains(MGC.coordinate2Point(p)))
					return true;
			}

			return false;
		}


		/**
		 * Return all features in the index.
		 */
		public List<SimpleFeature> getAll() {
			List<SimpleFeature> result = new ArrayList<>();
			itemsTree(result, index.getRoot());

			return result;
		}

		private void itemsTree(List<SimpleFeature> list, AbstractNode node) {
			for (Object o : node.getChildBoundables()) {
				Boundable childBoundable = (Boundable) o;
				if (childBoundable instanceof AbstractNode) {
					itemsTree(list, (AbstractNode) childBoundable);
				} else if (childBoundable instanceof ItemBoundable) {
					list.add((SimpleFeature) ((ItemBoundable) childBoundable).getItem());
				} else {
					Assert.shouldNeverReachHere();
				}
			}
		}

		/**
		 * Size of the tree.
		 */
		public int size() {
			return index.size();
		}
	}

}
