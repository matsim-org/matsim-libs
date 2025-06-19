package org.matsim.application.options;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.data.*;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.geometry.jts.JTS;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.AbstractNode;
import org.locationtech.jts.index.strtree.Boundable;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reusable class for shape file options.
 *
 * @see CommandLine.Mixin
 */
public final class ShpOptions {

	/**
	 * Special value for {@link #createIndex(String, String)} to use the same crs as the shape file.
	 */
	public static final String SAME_CRS = "same_crs";

	private static final Logger log = LogManager.getLogger(ShpOptions.class);

	@CommandLine.Option(names = "--shp", description = "Optional path to shape file used for filtering", required = false)
	private String shp;

	@CommandLine.Option(names = "--shp-crs", description = "Overwrite coordinate system of the shape file")
	private String shpCrs;

	@CommandLine.Option(names = "--shp-layer", description = "Layer to use (for .gpgk files). Defaults to first layer.", required = false)
	private String shpLayer;

	@CommandLine.Option(names = "--shp-charset", description = "Charset used to read the shape file", defaultValue = "ISO-8859-1")
	private Charset shpCharset;

	public ShpOptions() {
	}

	/**
	 * Constructor to use shape options manually.
	 */
	public ShpOptions(Path shp, @Nullable String shpCrs, @Nullable Charset shpCharset) {
		this.shp = shp == null ? null : shp.toString();
		this.shpCrs = shpCrs;
		this.shpCharset = shpCharset;
	}

	/**
	 * Constructor to use shape options manually.
	 */
	public ShpOptions(String shp, @Nullable String shpCrs, @Nullable Charset shpCharset) {
		this.shp = shp;
		this.shpCrs = shpCrs;
		this.shpCharset = shpCharset;
	}

	private ShpOptions(String shp, String shpCrs, String shpLayer, Charset shpCharset) {
		this.shp = shp;
		this.shpCrs = shpCrs;
		this.shpLayer = shpLayer;
		this.shpCharset = shpCharset;
	}

	/**
	 * Create shp options with a specific layer. (Usually for gpkg files).
	 */
	public static ShpOptions ofLayer(String shp, @Nullable String shpLayer) {
		return new ShpOptions(shp, null, shpLayer, null);
	}

	/**
	 * Opens datastore to a shape-file.
	 */
	public static DataStore openDataStore(String shp) throws IOException {

		FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();

		URL url = IOUtils.resolveFileOrResource(shp);

		DataStore ds;
		if (shp.endsWith(".shp"))
			ds = factory.createDataStore(url);
		else if (shp.endsWith(".gpkg")) {

			// GeoPackage does not work with URLs, need to download it first
			if (url.getProtocol().startsWith("http") || url.getProtocol().startsWith("jar")) {

				String name = FilenameUtils.getBaseName(url.getFile());

				Path tmp = Files.createTempFile(name, ".gpkg");
				Files.copy(url.openStream(), tmp, StandardCopyOption.REPLACE_EXISTING);
				tmp.toFile().deleteOnExit();

				shp = tmp.toString();
			}

			ds = DataStoreFinder.getDataStore(Map.of(
				GeoPkgDataStoreFactory.DBTYPE.key, "geopkg",
				GeoPkgDataStoreFactory.DATABASE.key, shp,
				GeoPkgDataStoreFactory.READ_ONLY.key, true
			));
		} else if (shp.endsWith(".dbf") || shp.endsWith(".shx")) {
			throw new IllegalArgumentException("Shape file must be .shp, but was: " + shp);
		} else if (shp.endsWith(".zip")) {

			// Zip files will only work with local files
			URI uri;
			try (ZipInputStream zip = new ZipInputStream(IOUtils.getInputStream(url))) {

				ZipEntry entry;
				while ((entry = zip.getNextEntry()) != null) {
					if (entry.getName().endsWith(".shp"))
						break;
				}

				if (entry == null)
					throw new IllegalArgumentException("No .shp file found in the zip.");

				log.info("Using {} from {}", entry.getName(), shp);
				uri = new URI("jar:" + url + "!/" + entry.getName());
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Could not create URI for zip file: " + url, e);
			}

			ds = factory.createDataStore(uri.toURL());
		} else {
			throw new IllegalArgumentException("Shape file must either be .zip or .shp, but was: " + shp);
		}

		return Objects.requireNonNull(ds, "Could not create data store.");
	}

	/**
	 * Return whether a shape was set.
	 */
	public boolean isDefined() {
		return shp != null && !shp.isBlank() && !shp.equals("none");
	}

	/**
	 * Get the provided input crs.
	 */
	public String getShapeFile() {
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

		try {
			DataStore ds = openDataStore(shp);
			if (shpCharset != null && ds instanceof ShapefileDataStore shpDs)
				shpDs.setCharset(shpCharset);

			if (ds instanceof FileDataStore fds) {
				return GeoFileReader.getSimpleFeatures(fds);
			}

			return GeoFileReader.getSimpleFeatures(ds, getLayer(ds));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Return the union of all geometries in the shape file.
	 */
	public Geometry getGeometry() {

		Collection<SimpleFeature> features = readFeatures();
		if (features.isEmpty()) {
			throw new IllegalStateException("There is no feature in the shape file. Aborting...");
		}

		if (features.size() == 1) {
			return (Geometry) features.iterator().next().getDefaultGeometry();
		}

		GeometryFactory factory = ((Geometry) features.iterator().next().getDefaultGeometry()).getFactory();

		GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(
			features.stream()
				.filter(f -> f.getDefaultGeometry() != null)
				.map(f -> (Geometry) f.getDefaultGeometry()).toList()
		);

		if (geometryCollection.isEmpty()) {
			throw new IllegalStateException("There are no geometries in the shape file.");
		}

		return geometryCollection.union();
	}

	/**
	 * Return the union of all geometries in the shape file and project it to the target crs.
	 *
	 * @param toCRS target coordinate system
	 */
	public Geometry getGeometry(String toCRS) {
		try {
			CoordinateReferenceSystem sourceCRS = CRS.decode(detectCRS());
			CoordinateReferenceSystem targetCRS = CRS.decode(toCRS);

			MathTransform ct = CRS.findMathTransform(sourceCRS, targetCRS);

			return JTS.transform(getGeometry(), ct);

		} catch (TransformException | FactoryException e) {
			throw new IllegalStateException("Could not transform coordinates of the provided shape", e);
		}
	}

	/**
	 * Creates an index to query features from this shape file.
	 *
	 * @param queryCRS coordinate system of the queries
	 * @param attr     the attribute to query from the shape file
	 * @param filter   filter features by attribute values
	 */
	public Index createIndex(String queryCRS, String attr, Predicate<SimpleFeature> filter) {

		if (!isDefined())
			throw new IllegalStateException("Shape file path not specified");
		if (queryCRS == null)
			throw new IllegalArgumentException("Query crs must not be null!");

		try {
			DataStore ds = openDataStoreAndSetCRS();
			CoordinateTransformation ct;
			if (queryCRS.equals(SAME_CRS))
				ct = new IdentityTransformation();
			else {
				ct = TransformationFactory.getCoordinateTransformation(queryCRS, shpCrs);
			}
			return new Index(ct, ds, attr, filter);
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
	 * Create an index without a filter and the same query crs as the shape file.
	 *
	 * @see #createIndex(String, String)
	 */
	public Index createIndex(String attr) {
		return createIndex(SAME_CRS, attr, null);
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
				DataStore ds = openDataStore(shp);
				CoordinateReferenceSystem crs = ds.getSchema(getLayer(ds)).getCoordinateReferenceSystem();
				ds.dispose();
				shpCrs = "EPSG:" + CRS.lookupEpsgCode(crs, true);
				log.info("Using detected crs for {}: {}", shp, shpCrs);

			} catch (IOException | FactoryException | NullPointerException e) {
				throw new IllegalStateException("Could not determine crs of the shape file. Try to specify it manually using --shp-crs.", e);
			}
		}

		return shpCrs;
	}

	/**
	 * Open the shape file for processing and set the crs if not already specified.
	 */
	private DataStore openDataStoreAndSetCRS() throws IOException {
		DataStore ds = openDataStore(shp);

		if (shpCrs == null) {
			try {
				CoordinateReferenceSystem crs = ds.getSchema(getLayer(ds)).getCoordinateReferenceSystem();
				shpCrs = "EPSG:" + CRS.lookupEpsgCode(crs, true);
				log.info("Using detected crs for {}: {}", shp, shpCrs);
			} catch (FactoryException | NullPointerException e) {
				throw new IllegalStateException("Could not determine crs of the shape file. Try to specify it manually using --shp-crs.", e);
			}
		}

		return ds;
	}

	/**
	 * Get the selected layer or throw exception if not found.
	 */
	private String getLayer(DataStore ds) throws IOException {
		String[] typeNames = ds.getTypeNames();
		if (shpLayer != null) {
			if (!ArrayUtils.contains(typeNames, shpLayer))
				throw new IllegalArgumentException("Layer " + shpLayer + " not found in shape file.");

			return shpLayer;
		}

		return typeNames[0];
	}

	/**
	 * Create an inverse coordinate transformation from the shape file crs. Tries to autodetect the crs of the shape file.
	 */
	public CoordinateTransformation createInverseTransformation(String toCRS) {
		return TransformationFactory.getCoordinateTransformation(detectCRS(), toCRS);
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
		Index(CoordinateTransformation ct, DataStore ds, String attr, @Nullable Predicate<SimpleFeature> filter) throws IOException {
			if (shpCharset != null && ds instanceof ShapefileDataStore shpDs)
				shpDs.setCharset(shpCharset);

			FeatureReader<SimpleFeatureType, SimpleFeature> it;
			DefaultTransaction transaction = new DefaultTransaction();
			if (ds instanceof FileDataStore fds) {
				it = fds.getFeatureReader();
			} else {
				it = ds.getFeatureReader(new Query(getLayer(ds)), transaction);
			}

			while (it.hasNext()) {
				SimpleFeature ft = it.next();

				if (ft.getDefaultGeometry() == null) {
					log.warn("Feature {} has no geometry", ft);
					continue;
				}

				if (filter != null && !filter.test(ft))
					continue;

				Geometry geom = (Geometry) ft.getDefaultGeometry();
				Envelope env = geom.getEnvelopeInternal();
				index.insert(env, ft);
			}

			index.build();

			transaction.close();
			it.close();
			ds.dispose();

			//log.info("Created index with size: {}, depth: {}", index.size(), index.depth());

			this.ct = ct;
			this.attr = attr;
		}

		/**
		 * Query the index for first feature including matching the coordinate and return specified attribute.
		 *
		 * @return null when no features was found that contains the point
		 */
		@Nullable
		@SuppressWarnings("unchecked")
		public <T> T query(Coord coord) {
			SimpleFeature ft = queryFeature(coord);
			if (ft != null)
				return (T) ft.getAttribute(attr);

			return null;
			// throw new NoSuchElementException(String.format("No matching entry found for x:%f y:%f %s", x, y, p));
		}

		/**
		 * Query the index and return the whole feature.
		 */
		@Nullable
		@SuppressWarnings("unchecked")
		public SimpleFeature queryFeature(Coord coord) {
			// Because we can not easily transform the feature geometry with MATSim we have to do it the other way around...
			Coordinate p = MGC.coord2Coordinate(ct.transform(coord));

			List<SimpleFeature> result = index.query(new Envelope(p));
			for (SimpleFeature ft : result) {
				Geometry geom = (Geometry) ft.getDefaultGeometry();

				// Catch Exception for invalid, too complex geometries
				try {
					if (geom.contains(MGC.coordinate2Point(p)))
						return ft;
				} catch (TopologyException e) {
					if (geom.convexHull().contains(MGC.coordinate2Point(p)))
						return ft;
				}
			}

			return null;
		}

		/**
		 * Checks whether a coordinate is contained in any of the features.
		 */
		@SuppressWarnings("unchecked")
		public boolean contains(Coord coord) {
			return queryFeature(coord) != null;
		}

		/**
		 * Return all features in the index.
		 */
		public List<SimpleFeature> getAllFeatures() {
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

		/**
		 * Return underlying shp file. Should be used carefully.
		 */
		public ShpOptions getShp() {
			return ShpOptions.this;
		}

	}

}
